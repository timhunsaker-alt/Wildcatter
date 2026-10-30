package dev.wildcatter.solver;

/** A complex number: a phasor (RMS magnitude and angle) or an impedance. Plain Java, no Minecraft. */
public record Complex(double re, double im) {
	public static final Complex ZERO = new Complex(0, 0);
	public static final Complex ONE = new Complex(1, 0);

	/** A phasor from its magnitude and angle in degrees. */
	public static Complex polar(double magnitude, double degrees) {
		double r = Math.toRadians(degrees);
		return new Complex(magnitude * Math.cos(r), magnitude * Math.sin(r));
	}

	public Complex plus(Complex o) {
		return new Complex(re + o.re, im + o.im);
	}

	public Complex minus(Complex o) {
		return new Complex(re - o.re, im - o.im);
	}

	public Complex times(Complex o) {
		return new Complex(re * o.re - im * o.im, re * o.im + im * o.re);
	}

	public Complex times(double k) {
		return new Complex(re * k, im * k);
	}

	public Complex dividedBy(Complex o) {
		double d = o.re * o.re + o.im * o.im;
		return new Complex((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d);
	}

	public Complex conjugate() {
		return new Complex(re, -im);
	}

	public Complex negate() {
		return new Complex(-re, -im);
	}

	public double abs() {
		return Math.hypot(re, im);
	}

	/** Angle in degrees, from -180 to 180. */
	public double degrees() {
		return Math.toDegrees(Math.atan2(im, re));
	}
}
