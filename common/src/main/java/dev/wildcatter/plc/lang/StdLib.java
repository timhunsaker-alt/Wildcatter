package dev.wildcatter.plc.lang;

/**
 * IEC 61131-3's standard function blocks, written in structured text and compiled with every project, so they cost instructions as a real
 * CPU's do. PID, SCALE, ALARM_HL and TOTALIZER are not in the standard; every real vendor adds their like.
 */
final class StdLib {
	private StdLib() {
	}

	static final String SOURCE = """
			FUNCTION_BLOCK SR
			VAR_INPUT S1, R : BOOL; END_VAR
			VAR_OUTPUT Q1 : BOOL; END_VAR
			Q1 := S1 OR (NOT R AND Q1);
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK RS
			VAR_INPUT S, R1 : BOOL; END_VAR
			VAR_OUTPUT Q1 : BOOL; END_VAR
			Q1 := NOT R1 AND (S OR Q1);
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK R_TRIG
			VAR_INPUT CLK : BOOL; END_VAR
			VAR_OUTPUT Q : BOOL; END_VAR
			VAR M : BOOL; END_VAR
			Q := CLK AND NOT M;
			M := CLK;
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK F_TRIG
			VAR_INPUT CLK : BOOL; END_VAR
			VAR_OUTPUT Q : BOOL; END_VAR
			VAR M : BOOL := TRUE; END_VAR
			Q := NOT CLK AND NOT M;
			M := NOT CLK;
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK CTU
			VAR_INPUT CU, R : BOOL; PV : INT; END_VAR
			VAR_OUTPUT Q : BOOL; CV : INT; END_VAR
			VAR LAST : BOOL; END_VAR
			IF R THEN
				CV := 0;
			ELSIF CU AND NOT LAST AND CV < 32767 THEN
				CV := CV + 1;
			END_IF;
			LAST := CU;
			Q := CV >= PV;
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK CTD
			VAR_INPUT CD, LD : BOOL; PV : INT; END_VAR
			VAR_OUTPUT Q : BOOL; CV : INT; END_VAR
			VAR LAST : BOOL; END_VAR
			IF LD THEN
				CV := PV;
			ELSIF CD AND NOT LAST AND CV > -32768 THEN
				CV := CV - 1;
			END_IF;
			LAST := CD;
			Q := CV <= 0;
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK CTUD
			VAR_INPUT CU, CD, R, LD : BOOL; PV : INT; END_VAR
			VAR_OUTPUT QU, QD : BOOL; CV : INT; END_VAR
			VAR LASTU, LASTD : BOOL; END_VAR
			IF R THEN
				CV := 0;
			ELSIF LD THEN
				CV := PV;
			ELSE
				IF CU AND NOT LASTU AND CV < 32767 THEN
					CV := CV + 1;
				END_IF;
				IF CD AND NOT LASTD AND CV > -32768 THEN
					CV := CV - 1;
				END_IF;
			END_IF;
			LASTU := CU;
			LASTD := CD;
			QU := CV >= PV;
			QD := CV <= 0;
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK TON
			VAR_INPUT IN : BOOL; PT : TIME; END_VAR
			VAR_OUTPUT Q : BOOL; ET : TIME; END_VAR
			VAR START : TIME; RUNNING : BOOL; END_VAR
			IF NOT IN THEN
				RUNNING := FALSE;
				ET := T#0s;
				Q := FALSE;
			ELSE
				IF NOT RUNNING THEN
					RUNNING := TRUE;
					START := _NOW();
				END_IF;
				ET := MIN(_NOW() - START, PT);
				Q := ET >= PT;
			END_IF;
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK TOF
			VAR_INPUT IN : BOOL; PT : TIME; END_VAR
			VAR_OUTPUT Q : BOOL; ET : TIME; END_VAR
			VAR START : TIME; RUNNING : BOOL; END_VAR
			IF IN THEN
				RUNNING := FALSE;
				ET := T#0s;
				Q := TRUE;
			ELSE
				IF NOT RUNNING AND Q THEN
					RUNNING := TRUE;
					START := _NOW();
				END_IF;
				IF RUNNING THEN
					ET := MIN(_NOW() - START, PT);
					Q := ET < PT;
				END_IF;
			END_IF;
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK TP
			VAR_INPUT IN : BOOL; PT : TIME; END_VAR
			VAR_OUTPUT Q : BOOL; ET : TIME; END_VAR
			VAR START : TIME; LAST : BOOL; END_VAR
			IF IN AND NOT LAST AND NOT Q THEN
				Q := TRUE;
				START := _NOW();
			END_IF;
			IF Q THEN
				ET := MIN(_NOW() - START, PT);
				IF ET >= PT THEN
					Q := FALSE;
				END_IF;
			ELSIF NOT IN THEN
				ET := T#0s;
			END_IF;
			LAST := IN;
			END_FUNCTION_BLOCK

			FUNCTION SCALE : REAL
			VAR_INPUT IN, IN_LO, IN_HI, OUT_LO, OUT_HI : REAL; END_VAR
			IF IN_HI = IN_LO THEN
				SCALE := OUT_LO;
			ELSE
				SCALE := OUT_LO + (IN - IN_LO) * (OUT_HI - OUT_LO) / (IN_HI - IN_LO);
			END_IF;
			END_FUNCTION

			FUNCTION_BLOCK ALARM_HL
			VAR_INPUT PV, HI, LO, DB : REAL; END_VAR
			VAR_OUTPUT HIGH, LOW : BOOL; END_VAR
			IF PV >= HI THEN
				HIGH := TRUE;
			ELSIF PV < HI - DB THEN
				HIGH := FALSE;
			END_IF;
			IF PV <= LO THEN
				LOW := TRUE;
			ELSIF PV > LO + DB THEN
				LOW := FALSE;
			END_IF;
			END_FUNCTION_BLOCK

			FUNCTION_BLOCK TOTALIZER
			VAR_INPUT RATE : REAL; RESET : BOOL; END_VAR
			VAR_OUTPUT TOTAL : LREAL; END_VAR
			VAR LAST : TIME; STARTED : BOOL; END_VAR
			IF RESET THEN
				TOTAL := 0.0;
			ELSIF STARTED THEN
				TOTAL := TOTAL + REAL_TO_LREAL(RATE) * TIME_TO_LREAL(_NOW() - LAST) / 1000.0;
			END_IF;
			LAST := _NOW();
			STARTED := TRUE;
			END_FUNCTION_BLOCK

			(* A positional PID: derivative on the measurement, so a setpoint step gives no kick; the integral stops at the output limits, so it
			   does not wind up; and in manual the integral tracks the output and the setpoint tracks the measurement, so going to auto does not bump. *)
			FUNCTION_BLOCK PID
			VAR_INPUT
				AUTO : BOOL := TRUE;
				PV, SP : REAL;
				MAN : REAL;
				KP : REAL := 1.0;
				TI, TD : TIME;
				OUT_MIN : REAL := 0.0;
				OUT_MAX : REAL := 100.0;
				REVERSE : BOOL;
				SP_RAMP : REAL;
			END_VAR
			VAR_OUTPUT OUT, ERR, SP_ACT : REAL; SAT : BOOL; END_VAR
			VAR INTEGRAL, LAST_PV, DT, GAIN, P, D, RAW : REAL; LAST : TIME; STARTED : BOOL; END_VAR
			DT := TIME_TO_REAL(_NOW() - LAST) / 1000.0;
			LAST := _NOW();
			GAIN := SEL(REVERSE, KP, -KP);
			IF NOT STARTED THEN
				LAST_PV := PV;
				SP_ACT := PV;
				DT := 0.0;
				STARTED := TRUE;
			END_IF;
			IF NOT AUTO THEN
				OUT := LIMIT(OUT_MIN, MAN, OUT_MAX);
				SP_ACT := PV;
				ERR := 0.0;
				INTEGRAL := OUT;
				SAT := FALSE;
			ELSE
				IF SP_RAMP > 0.0 THEN
					SP_ACT := LIMIT(SP_ACT - SP_RAMP * DT, SP, SP_ACT + SP_RAMP * DT);
				ELSE
					SP_ACT := SP;
				END_IF;
				ERR := SP_ACT - PV;
				P := GAIN * ERR;
				D := 0.0;
				IF TD > T#0s AND DT > 0.0 THEN
					D := -GAIN * TIME_TO_REAL(TD) / 1000.0 * (PV - LAST_PV) / DT;
				END_IF;
				RAW := P + INTEGRAL + D;
				IF TI > T#0s THEN
					IF RAW > OUT_MIN AND RAW < OUT_MAX OR (RAW >= OUT_MAX AND P < 0.0) OR (RAW <= OUT_MIN AND P > 0.0) THEN
						INTEGRAL := LIMIT(OUT_MIN, INTEGRAL + GAIN * ERR * DT / (TIME_TO_REAL(TI) / 1000.0), OUT_MAX);
					END_IF;
					RAW := P + INTEGRAL + D;
				END_IF;
				OUT := LIMIT(OUT_MIN, RAW, OUT_MAX);
				SAT := RAW <> OUT;
			END_IF;
			LAST_PV := PV;
			END_FUNCTION_BLOCK

			TYPE
				SFC_STEP : STRUCT
					X : BOOL;
					T : TIME;
					START : TIME;
				END_STRUCT;
			END_TYPE

			(* The standard's action control: a qualifier's rule for when an action is on. Q is on while it is; A is on while its body runs, which
			   is Q and one scan more, so the body can tidy up. S0 is the standard's S, a word this block keeps for its set-reset. *)
			FUNCTION_BLOCK ACTION_CONTROL
			VAR_INPUT N, R, S0, L, D, P, P1, P0, SD, DS, SL : BOOL; T : TIME; END_VAR
			VAR_OUTPUT Q, A : BOOL; END_VAR
			VAR
				__S_FF, __SD_FF, __DS_FF, __SL_FF : RS;
				__L_T, __D_T, __SD_T, __DS_T, __SL_T : TON;
				__P_E, __P1_E : R_TRIG;
				__P0_E, __Q_E : F_TRIG;
			END_VAR
			__S_FF(S := S0, R1 := R);
			__L_T(IN := L, PT := T);
			__D_T(IN := D, PT := T);
			__SD_FF(S := SD, R1 := R);
			__SD_T(IN := __SD_FF.Q1, PT := T);
			__DS_T(IN := DS, PT := T);
			__DS_FF(S := __DS_T.Q, R1 := R);
			__SL_T(IN := __SL_FF.Q1, PT := T);
			__SL_FF(S := SL, R1 := R OR __SL_T.Q);
			__P_E(CLK := P);
			__P1_E(CLK := P1);
			__P0_E(CLK := P0);
			Q := NOT R AND (N OR (L AND NOT __L_T.Q) OR __D_T.Q OR __P_E.Q OR __S_FF.Q1 OR __SD_T.Q OR __DS_FF.Q1 OR __SL_FF.Q1);
			__Q_E(CLK := Q);
			A := Q OR __Q_E.Q OR __P1_E.Q OR __P0_E.Q;
			END_FUNCTION_BLOCK

			(* A motor, as a real vendor's device block: demand in, RUN out, a fault latched on what it sees. It cannot hold off a trip: the
			   overload acts in the starter whatever this says. Outside AUTO the motor is not the program's, so it asks for nothing and raises nothing. *)
			FUNCTION_BLOCK MOTOR_CTRL
			VAR_INPUT
				START : BOOL;
				PERMIT : BOOL := TRUE;
				RUNNING, TRIPPED : BOOL;
				AUTO : BOOL := TRUE;
				RESET : BOOL;
				START_T : TIME := T#15s;
				STOP_T : TIME := T#15s;
			END_VAR
			VAR_OUTPUT RUN, FAULT, FAIL_START, FAIL_STOP, FAIL_RUN : BOOL; STATE : INT; END_VAR
			VAR __WAIT : TON; __UP : BOOL; END_VAR
			IF RESET THEN
				FAULT := FALSE;
				FAIL_START := FALSE;
				FAIL_STOP := FALSE;
				FAIL_RUN := FALSE;
			END_IF;
			IF NOT AUTO THEN
				RUN := FALSE;
				__UP := FALSE;
				__WAIT(IN := FALSE, PT := START_T);
				STATE := 5;
			ELSE
				IF TRIPPED THEN
					FAULT := TRUE;
				END_IF;
				RUN := START AND PERMIT AND NOT FAULT;
				IF RUN AND RUNNING THEN
					__UP := TRUE;
				ELSIF RUN AND __UP THEN
					FAIL_RUN := TRUE;
					FAULT := TRUE;
					RUN := FALSE;
				END_IF;
				IF NOT RUN THEN
					__UP := FALSE;
				END_IF;
				__WAIT(IN := RUN <> RUNNING, PT := SEL(RUN, STOP_T, START_T));
				IF __WAIT.Q THEN
					IF RUN THEN
						FAIL_START := TRUE;
					ELSE
						FAIL_STOP := TRUE;
					END_IF;
					FAULT := TRUE;
					RUN := FALSE;
				END_IF;
				IF FAULT THEN
					STATE := 4;
				ELSIF RUN AND RUNNING THEN
					STATE := 2;
				ELSIF RUN THEN
					STATE := 1;
				ELSIF RUNNING THEN
					STATE := 3;
				ELSE
					STATE := 0;
				END_IF;
			END_IF;
			END_FUNCTION_BLOCK

			(* A valve, for either kind (docs/multiblocks/plc.md): OUT held for a spring-return solenoid, CMD_OPEN and CMD_CLOSE driven to the limit for
			   a motor-operated one. A fault drops them all, so an air valve goes to its fail position and a motor-operated one stays put. *)
			FUNCTION_BLOCK VALVE_CTRL
			VAR_INPUT
				OPEN : BOOL;
				PERMIT : BOOL := TRUE;
				OPENED, CLOSED : BOOL;
				AUTO : BOOL := TRUE;
				RESET : BOOL;
				TRAVEL_T : TIME := T#30s;
			END_VAR
			VAR_OUTPUT OUT, CMD_OPEN, CMD_CLOSE, FAULT, FAIL_OPEN, FAIL_CLOSE, BAD_LIMIT : BOOL; STATE : INT; END_VAR
			VAR __WAIT : TON; __THERE : BOOL; END_VAR
			IF RESET THEN
				FAULT := FALSE;
				FAIL_OPEN := FALSE;
				FAIL_CLOSE := FALSE;
				BAD_LIMIT := FALSE;
			END_IF;
			IF NOT AUTO THEN
				OUT := FALSE;
				CMD_OPEN := FALSE;
				CMD_CLOSE := FALSE;
				__WAIT(IN := FALSE, PT := TRAVEL_T);
				STATE := 5;
			ELSE
				IF OPENED AND CLOSED THEN
					BAD_LIMIT := TRUE;
					FAULT := TRUE;
				END_IF;
				OUT := OPEN AND PERMIT AND NOT FAULT;
				__THERE := (OUT AND OPENED) OR (NOT OUT AND CLOSED);
				__WAIT(IN := NOT __THERE AND NOT FAULT, PT := TRAVEL_T);
				IF __WAIT.Q THEN
					IF OUT THEN
						FAIL_OPEN := TRUE;
					ELSE
						FAIL_CLOSE := TRUE;
					END_IF;
					FAULT := TRUE;
					OUT := FALSE;
				END_IF;
				CMD_OPEN := OUT AND NOT OPENED;
				CMD_CLOSE := NOT OUT AND NOT CLOSED AND NOT FAULT;
				IF FAULT THEN
					STATE := 4;
				ELSIF OUT AND __THERE THEN
					STATE := 2;
				ELSIF OUT THEN
					STATE := 1;
				ELSIF __THERE THEN
					STATE := 0;
				ELSE
					STATE := 3;
				END_IF;
			END_IF;
			END_FUNCTION_BLOCK
			""";
}
