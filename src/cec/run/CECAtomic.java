package cec.run;

import cec.cluster.Cluster;
import cec.cluster.ClusterLike;
import cec.cluster.types.ClusterKind;
import cec.cluster.types.TypeOptions;
import cec.cluster.types.gaussian.Gaussians;
import cec.input.Data;
import cec.options.CECConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;
import javafx.util.Pair;
import org.ejml.simple.SimpleMatrix;

/**
 *
 * @author Krzysztof
 */
public class CECAtomic {

    private final Random rand = new Random();
    private final double divideCnstraints;

    private final Data data;
    private final List<Pair<ClusterKind, TypeOptions>> clusterTypes;
    private final int iterations;
    final int SIZE_MIN;
    /**
     * cost per iteration
     */
    private final List<Double> costs;
    private final ArrayList<Cluster> clusters;
    /**
     * cost per cluster
     */
    private double[] cost;
    private int numberOfClusters;

    public CECAtomic(Data data, List<Pair<ClusterKind, TypeOptions>> clusterTypes, int iterations) {
        this.data = data;
        this.clusterTypes = clusterTypes;
        this.iterations = iterations;
        this.costs = new ArrayList<>();

        this.numberOfClusters = clusterTypes.size();
        this.cost = new double[numberOfClusters];
        this.clusters = new ArrayList<>();

        this.SIZE_MIN = data.getSize() / 100;
        this.divideCnstraints = CECConfig.getInstance().getCCECConstraints();
        fillClusters();
    }

    public double getCost() {
//        double s = 0;
//        for (double d : cost) {
//            s += d;
//        }
//        return s;
        return DoubleStream.of(cost).sum();
    }

    private void fillClusters() {
        clusterTypes.stream().forEach((Pair<ClusterKind, TypeOptions> p) -> {
            if (p.getKey().isOptionNeeded()) {
                clusters.add(new Cluster(p.getKey().getFunction().setOptions(p.getValue()), data.getDimension()));
            } else {
                clusters.add(new Cluster(p.getKey().getFunction(), data.getDimension()));
            }
        });
    }

    public int getDimension() {
        return data.getDimension();
    }

    public Data getData() {
        return data;
    }

    /**
     *
     * @return list of cluters
     */
    public List<Cluster> getCLusters() {
        return clusters;
    }

    /**
     * initialize the CEC - set id for a clusters - randomly set points to
     * cluster - calcualte initial cost
     */
    private void init() {

        for (int i = 0; i < clusters.size(); ++i) {
            clusters.get(i).setId(i);
        }

        data.getData().stream().forEach((p) -> {
            clusters.get(rand.nextInt(numberOfClusters)).add(p);
        });

        for (int i = 0; i < numberOfClusters; ++i) {
            cost[i] = clusters.get(i).getCost();
        }
    }

    private boolean iteration() {
        final double cost_ret = getCost();

        for (Cluster Yj : clusters) {
            for (ClusterLike p : Yj.getData()) {
                final double Yj_cost_sub = Yj.sub(p, false).getCost();
                int best_cluster = Yj.getId();
                double best_cost = Double.MIN_VALUE;

                for (Cluster Yi : clusters) {
                    if (Yi.isEmpty() || Yi.getId() == Yj.getId()) {
                        continue;
                    }

                    final double local_cost = cost[Yj.getId()] + cost[Yi.getId()] - Yi.add(p, false).getCost() - Yj_cost_sub;

                    if (local_cost > 0 && best_cost < local_cost) {
                        best_cost = local_cost;
                        best_cluster = Yi.getId();
                    }
                    Yi.sub(p, false);
                }

                if (Yj.getId() != best_cluster) {
                    cost[Yj.getId()] = Yj.subPoint(p).getCost();//
                    cost[best_cluster] = clusters.get(best_cluster).add(p).getCost();
                } else {
                    Yj.add(p, false);
                }

                //delete cluster
                if (!Yj.isEmpty() && Yj.getCardinality() < SIZE_MIN) {
//                    System.out.println("-----delete----- " + Yj.getId());
                    Yj.getData().stream().forEach((p_del) -> {
                        clusters.get(getRandomCluster(Yj.getId())).add(p_del);
                    });

                    Yj.clear();

                    //update cost
                    for (int i = 0; i < numberOfClusters; ++i) {
                        cost[i] = clusters.get(i).getCost();
                    }
                    break;
                }
            }
        }
//        System.out.println("iter " + getCost());
        return cost_ret != getCost();
    }

    private int getRandomCluster(int x) {
        int ret = 0;
        do {
            ret = rand.nextInt(numberOfClusters);
        } while (x == ret || clusters.get(ret).isEmpty());
        return ret;
    }

    public void run() {
        //initialization
        init();

        for (int i = 0; i < iterations; ++i) {
            final boolean t = iteration();
            costs.add(getCost());
            if (!t) {
                break;
            }

            //zmiana na dzielenie co ileś pętli
            divide(i);
        }
    }

    private void simpleRun() {
        //initialization
        init();

        for (int i = 0; i < iterations; ++i) {
            final boolean t = iteration();
            costs.add(getCost());
            if (!t) {
                break;
            }
        }
    }

    /**
     *
     * @return initail number of clusters (it can differ from result clasuters
     * needed for data description)
     */
    public int getNumberOfClusters() {
        return numberOfClusters;
    }

    /**
     *
     * @return cluster needed for fully describe data
     */
    private int getUsedNumberOfClusters() {
        int ret = 0;
        ret = clusters.stream().filter((c) -> (c.isEmpty())).map((_item) -> 1).reduce(ret, Integer::sum);
        return ret;
    }

    /**
     * prints the results on console
     */
    public void showResults() {
        System.out.println("");
        System.out.println("BEST RUN INFO");
        System.out.println("Completted in " + costs.size() + " steps");
        System.out.println("Cost in each step " + costs.toString());
        final int v = getUsedNumberOfClusters();
        System.out.println(v + " needed for clustering (while " + numberOfClusters + " suggested)");

        int no = 0;
        for (Cluster c : clusters) {
            if (c.isEmpty()) {
                continue;
            }
            System.out.println("------------------------------------------------------------------");
            System.out.println("Cluster " + no++ + " " + c.getType());
            System.out.println(c.getCardinality() + " points");
            System.out.println("mean");
            System.out.println(c.getMean());
            System.out.println("cov");
            System.out.println(c.getCostFunction().getCov());
        }
    }

    private void divide(int it) {
        if (it % 10 == 0) {
            List<Cluster> newClusters = new ArrayList<>();
            for (Cluster c : clusters) {
                if (!c.isEmpty() && DSC(c) > divideCnstraints) {
                    System.out.println("----divide--- " + c.getId());
                    //divide
                    List<Pair<ClusterKind, TypeOptions>> params = new ArrayList<>();
                    params.add(clusterTypes.get(c.getId()));
                    params.add(clusterTypes.get(c.getId()));

                    CECAtomic best_result = null;
                    Data loc = new Data(c.getData());
                    for (int i = 0; i < 16; ++i) {
                        final CECAtomic result = new CECAtomic(loc, params, 50);

                        result.simpleRun();

                        if (best_result == null || best_result.getCost() > result.getCost()) {
                            best_result = result;
                        }
                    }
                    if (best_result != null) {
                        newClusters.addAll(best_result.getCLusters());
                        c.clear();
                    }
                }
            }
            if (newClusters.size() > 0) {
                numberOfClusters += newClusters.size();
                clusters.addAll(newClusters);
                //update cost
                cost = new double[numberOfClusters];
                for (int i = 0; i < numberOfClusters; ++i) {
                    cost[i] = clusters.get(i).getCost();
                }
            }
        }
    }

    private double DSC(Cluster c) {
//        final SimpleMatrix sigma2 = c.getCostFunction().getCov().scale(2.);
//        final int dim = c.getDimension();
//        final SimpleMatrix zeros = new SimpleMatrix(dim, 1);
//        double ret = Math.log(Gaussians.N(dim, zeros, sigma2, zeros));
//        double CIP = 0;
//        CIP = c.getData().stream().map((x) -> Gaussians.N(dim, c.getMean(), sigma2, x.getMean())).reduce(CIP, (accumulator, _item) -> accumulator + _item);
//        CIP /= c.getCardinality();
//        CIP = -2. * Math.log(CIP);
//        double D = 0;
//        for (ClusterLike xi : c.getData()) {
//            for (ClusterLike xj : c.getData()) {
//                D += Gaussians.N(dim, zeros, sigma2, xi.getMean().minus(xj.getMean()));
//            }
//        }
//        D /= Math.pow(c.getCardinality(), 2);
//        D = Math.log(D);
//
////        System.out.println(ret + CIP + D);
//        return ret + CIP + D;
        final SimpleMatrix sigma2 = c.getCostFunction().getCov().scale(2.);
        final int dim = c.getDimension();
        final double card = c.getCardinality();
        final SimpleMatrix zeros = new SimpleMatrix(dim, 1);

        //ret
        final double ret = Math.log(Gaussians.N(dim, zeros, sigma2, zeros));

        //CIP
        double CIP = 0;
        CIP = c.getData().stream().map((x) -> Gaussians.N(dim, c.getMean(), sigma2, x.getMean())).reduce(CIP, (accumulator, _item) -> accumulator + _item);
        CIP /= card;
        CIP = -2. * Math.log(CIP);

        //D
        double D = Gaussians.N(dim, zeros, sigma2, zeros) * card;
        for (int i = 0; i < card; ++i) {
            SimpleMatrix p = c.getData().get(i).getMean();
            for (int j = i + 1; j < card; ++j) {
                D += 2. * Gaussians.N(dim, zeros, sigma2, p.minus(c.getData().get(j).getMean()));
            }
        }

//        for (ClusterLike xi : c.getData()) {
//            for (ClusterLike xj : c.getData()) {
//                D += Gaussians.N(dim, zeros, sigma2, xi.getMean().minus(xj.getMean()));
//            }
//        }
        D /= Math.pow(c.getCardinality(), 2);
        D = Math.log(D);

        return ret + CIP + D;
    }
}
