package cec.cluster.types.gaussian;

import cec.cluster.Cluster;
import cec.cluster.types.Cost;
import org.ejml.simple.SimpleMatrix;

/**
 *
 * @author Krzysztof
 */
public class Gaussians extends Cost {

    @Override
    public double h() {
        return cluster.getWeight() 
                * (
                    - Math.log(cluster.getWeight())
                    + cluster.getDimension() * 0.5 * Math.log(2. * Math.PI * Math.E)
                    + 0.5 * Math.log(cluster.getCov().determinant() * (cluster.getCardinality() - 1.) / cluster.getCardinality())
                );
    }

    @Override
    public String getInfo() {
        return "Gaussian: All Gaussian distributions";
    }

    @Override
    public SimpleMatrix getCov() {
        return cluster.getCov();
    }

    
    public static double N(int dim, SimpleMatrix mean, SimpleMatrix sigma, SimpleMatrix x){        
        final SimpleMatrix s = x.minus(mean);
        return Math.exp(-0.5 * s.transpose().mult(sigma.invert()).mult(s).get(0, 0))/(Math.pow(2. * Math.PI, dim/2.) * Math.sqrt(sigma.determinant()));
    }
}
