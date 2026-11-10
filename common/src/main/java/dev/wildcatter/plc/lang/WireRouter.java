package dev.wildcatter.plc.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

/**
 * Routes a function block diagram's wire around the blocks: out of an output pin to the right, into an input pin from the left, in straight
 * runs at right angles, none through a block. It searches the lines along the blocks' edges and between them (an orthogonal visibility grid)
 * for the shortest way with the fewest bends. Pure arithmetic, so the screen can cache it and a test can check it.
 */
public final class WireRouter {
	/** How far a wire keeps from a block. */
	public static final int MARGIN = 4;
	/** The straight piece out of a pin before a wire may turn. */
	public static final int STUB = 8;
	/** A bend costs as much as this much more length: fewer bends read better. */
	private static final int BEND = 16;
	private static final int[] DX = {1, -1, 0, 0};
	private static final int[] DY = {0, 0, 1, -1};

	private WireRouter() {
	}

	/** A block's box on the sheet. */
	public record Box(int x, int y, int w, int h) {
	}

	/**
	 * The wire from the output pin at ({@code x0}, {@code y0}) to the input pin at ({@code x1}, {@code y1}), as the corners of its path: x, y
	 * pairs, the first the output pin and the last the input pin. With no way around, it goes straight across, turning half way.
	 */
	public static int[] route(List<Box> boxes, int x0, int y0, int x1, int y1) {
		int sx = x0 + STUB;
		int gx = x1 - STUB;
		TreeSet<Integer> xs = new TreeSet<>(List.of(sx, gx));
		TreeSet<Integer> ys = new TreeSet<>(List.of(y0, y1));
		int[][] rects = new int[boxes.size()][];
		for (int k = 0; k < boxes.size(); k++) {
			Box b = boxes.get(k);
			rects[k] = new int[] {b.x() - MARGIN, b.y() - MARGIN, b.x() + b.w() + MARGIN, b.y() + b.h() + MARGIN};
			xs.add(rects[k][0]);
			xs.add(rects[k][2]);
			ys.add(rects[k][1]);
			ys.add(rects[k][3]);
		}
		// a channel between two blocks is best taken down its middle
		List<Integer> mids = new ArrayList<>();
		Integer last = null;
		for (int x : xs) {
			if (last != null && x - last > 2 * MARGIN) {
				mids.add((x + last) / 2);
			}
			last = x;
		}
		xs.addAll(mids);
		int[] X = xs.stream().mapToInt(Integer::intValue).toArray();
		int[] Y = ys.stream().mapToInt(Integer::intValue).toArray();
		int nx = X.length;
		int ny = Y.length;
		// an edge of the grid is blocked where it runs through a block's inside; along its margin is free
		boolean[] blockedH = new boolean[nx * ny];
		boolean[] blockedV = new boolean[nx * ny];
		for (int[] r : rects) {
			int i0 = Arrays.binarySearch(X, r[0]);
			int i1 = Arrays.binarySearch(X, r[2]);
			int j0 = Arrays.binarySearch(Y, r[1]);
			int j1 = Arrays.binarySearch(Y, r[3]);
			for (int j = j0 + 1; j < j1; j++) {
				for (int i = i0; i < i1; i++) {
					blockedH[j * nx + i] = true;
				}
			}
			for (int i = i0 + 1; i < i1; i++) {
				for (int j = j0; j < j1; j++) {
					blockedV[j * nx + i] = true;
				}
			}
		}
		int si = Arrays.binarySearch(X, sx);
		int sj = Arrays.binarySearch(Y, y0);
		int gi = Arrays.binarySearch(X, gx);
		int gj = Arrays.binarySearch(Y, y1);
		int states = nx * ny * 4;
		int[] cost = new int[states];
		int[] from = new int[states];
		Arrays.fill(cost, Integer.MAX_VALUE);
		PriorityQueue<long[]> open = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
		int start = (sj * nx + si) * 4;   // heading right, out of the pin
		cost[start] = 0;
		from[start] = -1;
		open.add(new long[] {heuristic(X, Y, si, sj, gi, gj), start});
		int found = -1;
		while (!open.isEmpty()) {
			long[] top = open.poll();
			int state = (int) top[1];
			int dir = state % 4;
			int node = state / 4;
			int i = node % nx;
			int j = node / nx;
			if (top[0] - heuristic(X, Y, i, j, gi, gj) > cost[state]) {
				continue;   // a stale entry
			}
			if (i == gi && j == gj) {
				found = state;
				break;
			}
			for (int nd = 0; nd < 4; nd++) {
				if ((nd ^ 1) == dir) {
					continue;   // no turning back on itself
				}
				int ni = i + DX[nd];
				int nj = j + DY[nd];
				if (ni < 0 || nj < 0 || ni >= nx || nj >= ny) {
					continue;
				}
				boolean blocked = switch (nd) {
					case 0 -> blockedH[j * nx + i];
					case 1 -> blockedH[j * nx + ni];
					case 2 -> blockedV[j * nx + i];
					default -> blockedV[nj * nx + i];
				};
				if (blocked) {
					continue;
				}
				int step = Math.abs(X[ni] - X[i]) + Math.abs(Y[nj] - Y[j]) + (nd != dir ? BEND : 0);
				int next = (nj * nx + ni) * 4 + nd;
				// arriving where the wire must head right into its pin: a last bend if it is not already
				int arrive = ni == gi && nj == gj && nd != 0 ? BEND : 0;
				if (cost[state] + step + arrive < cost[next]) {
					cost[next] = cost[state] + step + arrive;
					from[next] = state;
					open.add(new long[] {cost[next] + heuristic(X, Y, ni, nj, gi, gj), next});
				}
			}
		}
		if (found < 0) {
			int mid = x1 > x0 ? (x0 + x1) / 2 : x0 + STUB;
			return new int[] {x0, y0, mid, y0, mid, y1, x1, y1};
		}
		List<int[]> path = new ArrayList<>();
		for (int state = found; state >= 0; state = from[state]) {
			int node = state / 4;
			path.addFirst(new int[] {X[node % nx], Y[node / nx]});
		}
		path.addFirst(new int[] {x0, y0});
		path.add(new int[] {x1, y1});
		return corners(path);
	}

	private static long heuristic(int[] X, int[] Y, int i, int j, int gi, int gj) {
		return Math.abs(X[i] - X[gi]) + Math.abs(Y[j] - Y[gj]);
	}

	/** The path's corners only: points along a straight run are dropped. */
	private static int[] corners(List<int[]> path) {
		List<int[]> out = new ArrayList<>();
		for (int[] p : path) {
			if (!out.isEmpty() && out.getLast()[0] == p[0] && out.getLast()[1] == p[1]) {
				continue;
			}
			if (out.size() >= 2) {
				int[] a = out.get(out.size() - 2);
				int[] b = out.getLast();
				if ((a[0] == b[0] && b[0] == p[0]) || (a[1] == b[1] && b[1] == p[1])) {
					out.removeLast();
				}
			}
			out.add(p);
		}
		int[] flat = new int[out.size() * 2];
		for (int k = 0; k < out.size(); k++) {
			flat[2 * k] = out.get(k)[0];
			flat[2 * k + 1] = out.get(k)[1];
		}
		return flat;
	}

	/** True if the path's straight runs keep out of every box's inside, the pins' own short stubs aside. */
	public static boolean clear(int[] path, List<Box> boxes) {
		for (int k = 0; k + 3 < path.length; k += 2) {
			int ax = path[k];
			int ay = path[k + 1];
			int bx = path[k + 2];
			int by = path[k + 3];
			for (Box b : boxes) {
				boolean crosses = ay == by
						? b.y() < ay && ay < b.y() + b.h() && Math.min(ax, bx) < b.x() + b.w() && Math.max(ax, bx) > b.x()
						: b.x() < ax && ax < b.x() + b.w() && Math.min(ay, by) < b.y() + b.h() && Math.max(ay, by) > b.y();
				if (crosses) {
					return false;
				}
			}
		}
		return true;
	}
}
