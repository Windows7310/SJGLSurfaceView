package sjipano.com.abc;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * 全景图中坐标装换 工具栏类
 * Created by zhangkang on 2018/1/12.
 */

public class SJGLUtils {

    /**
     * 计算参数
     * @param fov
     * @return
     */
    private static double sji_calc_d(double fov) {
        return 0.5d + 0.5d * Math.tanh( fov*0.0143 - 2.0);
    }

    /**
     * 由二维坐标，映射到三维
     * @param x
     * @param y
     * @param fov
     * @param yaw
     * @param pitch
     * @param width
     * @param height
     * @return
     */
    public static double[] getXYZ(
            double x, double y,
            double fov,
            double yaw, double pitch,
            double width, double height) {

        double point[] = new double[3];
        double d;
        d = sji_calc_d(fov);

        // coshfov, cscfov
        double hfov, coshfov, cschfov;
        hfov = 0.5d * fov * PI / 180.d;
        coshfov = cos(hfov);
        cschfov = 1.0d / sin(hfov);

        // viewmat
        double rad_pitch, rad_yaw;
        double cp, sp, cy, sy;

        rad_pitch =  pitch * PI / 180.0d;
        rad_yaw =   yaw * PI / 180.d;
        cp =  cos(rad_pitch);
        sp =  sin(rad_pitch);
        cy =  cos(rad_yaw);
        sy =  sin(rad_yaw);

        double viewmat[] = new double[9];
        viewmat[0] = cy;        viewmat[1] = sy * sp;       viewmat[2] = sy * cp;
        viewmat[3] = 0.d;       viewmat[4] = cp;            viewmat[5] = -sp;
        viewmat[6] = -sy;       viewmat[7] = cy * sp;       viewmat[8] = cy * cp;

        double u, v;
        u = 2.0d * x / width - 1.0d;
        v = 1.0d - 2.0d * y / height;

        if (width > height)
            v = v * height / width;
        else
            u = u * width / height;

        // solve b equation
        double C1, UVC2, a, b, c, X, Y, Z;
        C1 = 1.0d / ( cschfov * (coshfov+d) );
        UVC2 = ( u*u + v*v ) * C1 * C1;
        a = UVC2 + 1.0d;
        b = -UVC2 * 2.0d * d;
        c = UVC2 * d * d -1.0d;
        Z = (-b -  Math.sqrt(b*b-4.0d*a*c)) / (2.0d*a);
        X = u*(d-Z)*C1;
        Y = v*(d-Z)*C1;

        point[0] = viewmat[0] * X + viewmat[1] * Y + viewmat[2] * Z;
        point[1] = viewmat[3] * X + viewmat[4] * Y + viewmat[5] * Z;
        point[2] = viewmat[6] * X + viewmat[7] * Y + viewmat[8] * Z;

        return point;
    }

    public static double[] getXY(
            double x, double y, double z,
            double fov,
            double yaw, double pitch,
            double width, double height) {

        double point[] = new double[2];

        double d;
        d = sji_calc_d(fov);

        // coshfov, cscfov
        double hfov, coshfov, cschfov;
        hfov =   (0.5d * fov * PI / 180.d);
        coshfov =  cos(hfov);
        cschfov = 1.0d / sin(hfov);

        // viewmat
        double rad_pitch, rad_yaw;
        double cp, sp, cy, sy;

        rad_pitch =  pitch * PI / 180.0d;
        rad_yaw =  yaw * PI / 180.d;
        cp =  cos(rad_pitch);
        sp =  sin(rad_pitch);
        cy =  cos(rad_yaw);
        sy =  sin(rad_yaw);

        double viewmat[] = new double[9];
        viewmat[0] = cy;        viewmat[1] = 0.d;   viewmat[2] = -sy;
        viewmat[3] = sy * sp;   viewmat[4] = cp;    viewmat[5] = cy * sp;
        viewmat[6] = sy * cp;   viewmat[7] = -sp;   viewmat[8] = cy * cp;

        double X, Y, Z;
        X = viewmat[0] * x + viewmat[1] * y + viewmat[2] * z;
        Y = viewmat[3] * x + viewmat[4] * y + viewmat[5] * z;
        Z = viewmat[6] * x + viewmat[7] * y + viewmat[8] * z;

        // @important
        if (Z > 0.0001d) {
            point[0] = -1;
            point[1] = -1;
            return point;
        }

        double u, v, S;
        S = cschfov * (coshfov+d) / (d-Z);
        u = X * S;
        v = Y * S;

        if (width > height)
            v = v * width / height;
        else
            u = u * height / width;

        u = (u + 1.0d) * 0.5d * width;
        v = (1.0d - v) * 0.5d * height;

        //@todo check const value 100f
        if (u > (width + 100d) || u < -100d || v > (height + 100d) || v < -100d) {
            point[0] = -1;
            point[1] = -1;
        } else {
            point[0] = u;
            point[1] = v;
        }
        return point;
    }
}
