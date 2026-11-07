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
			""";
}
