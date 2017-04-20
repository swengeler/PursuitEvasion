package MathForShadow;
import java.awt.*;
import java.lang.Object;
import MathForShadow.Determinant;
        import java.awt.Point;
/**
 * Created by jonty on 20/04/2017.
 */
    public class ShadowEventTests {

        public boolean test13(Point p, Point u, Point v){
           double[][] A= new double[][]  {{p.x, p.y, 1},
            {u.x,u.y, 1}, {v.x,v.y, 1}};


            if(Determinant.determinant(A,3)==0) {
                return true;
            }else
                 return false;
        }
        public boolean test23(Point p, Point u, Point v){
            if((u.y-v.y)*(p.x-u.x)==(p.y-u.y)*(u.x-v.x)) {
                return true;
            }else
                return false;
            }

        public boolean test33(Point p, Point q, Point u, Point v, Point w, Point z) {
// p and q are pursuers
            // x and v are obstacles
            // w z are the end of vision

            double[][] B= new double[][] {{u.y-p.y, p.x-u.x, u.x*p.y-p.x*u.y},
                    {v.y-q.y,q.x-v.x,v.x*q.y-q.x*v.y },
                    { z.y-w.y, w.x-z.x, z.x*w.y-w.x*z.y}};

            if(Determinant.determinant(B,3)==0){
                return true;
            }
            else
                return false;

            }
        public boolean test44(Point p, Point q,Point r, Point u, Point v, Point w ){
            // p and q and r are pursuers
            // x and v and w are obstacles

            double[][] C =new double[][] {{u.y-p.y, p.x-u.x, u.x*p.y-p.x*u.y},
                                            {v.y-q.y, q.x-v.x, v.x*q.y-q.x*v.y},
                                            {w.y-r.y, r.x-w.x, w.x*r.y-r.x*w.y } };

            if(Determinant.determinant(C,3)==0) {
                return
                    true;
            }else
                return false;
            }

        }

