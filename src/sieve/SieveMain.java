package sieve;

import gnu.trove.TIntArrayList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SieveMain {
    public static final int N = (int) 1E8;

    public static final int THREADS = 8;

    public static void main(String[] args) throws Exception {
        int flag = Integer.parseInt(args[0]);
        if (flag != 1 && flag != 2) {
            throw new IllegalArgumentException();
        }

        TIntArrayList list = new TIntArrayList(N);
        for (int i = 2; i <= N; i++) {
            list.add(i);
        }
        double est = list.size();
        System.out.println("p diff est_diff error1 size est_size error2");
        for (int i = (int) Math.sqrt(N); i >= 2; i--) {
            if (isPrime(i)) {
                int oldSize = list.size();
                list = filter(list, i, flag);
                int diff = oldSize - list.size();
                double estDiff;
                if (flag == 1) {
                    estDiff = est / i;
                } else {
                    if (N % i == 0) {
                        estDiff = est / i;
                    } else {
                        estDiff = est * 2 / i;
                    }
                }
                double error1 = estDiff / diff - 1;
                est -= estDiff;
                double error2 = est / list.size() - 1;
                System.out.printf("%d %d %.2f %.6f%% %d %.2f %.6f%%%n", i, diff, estDiff, error1 * 100,
                        list.size(), est, error2 * 100);
            }
        }

        threadPool.shutdown();
    }

    public static TIntArrayList filter(TIntArrayList list, int p, int flag) throws Exception {
        List<List<Integer>> tasks = new ArrayList<>();
        int partitionSize = ceilDiv(list.size(), THREADS);
        for (int i = 0; i < THREADS; i++) {
            tasks.add(Arrays.asList(i * partitionSize, i * partitionSize + partitionSize));
        }

        TIntArrayList totalResult = new TIntArrayList();

        List<Future<?>> futureList = new ArrayList<>();
        for (List<Integer> task : tasks) {
            Future<?> future = threadPool.submit(() -> {
                int from = task.get(0);
                int to = task.get(1);
                from = Math.min(from, list.size());
                to = Math.min(to, list.size());
                TIntArrayList result = new TIntArrayList();
                for (int i = from; i != to; i++) {
                    int x = list.get(i);
                    if (flag == 1) {
                        if (x % p != 0) {
                            result.add(x);
                        }
                    } else {
                        if (x % p != 0 && (N - x) % p != 0) {
                            result.add(x);
                        }
                    }
                }
                synchronized (totalResult) {
                    for (int i = 0; i < result.size(); i++) {
                        totalResult.add(result.get(i));
                    }
                }
            });
            futureList.add(future);
        }

        for (Future<?> it : futureList) {
            it.get();
        }

        return totalResult;
    }

    public static final ExecutorService threadPool = Executors.newFixedThreadPool(THREADS);

    public static boolean isPrime(int n) {
        return n > 0 && BigInteger.valueOf(n).isProbablePrime(100);
    }

    public static int ceilDiv(int x, int y) {
        return BigDecimal.valueOf(x).divide(BigDecimal.valueOf(y), 0, RoundingMode.CEILING).intValue();
    }
}
