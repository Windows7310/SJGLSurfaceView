package sjipano.com.abc;

import java.util.List;

/**
 * @author: user
 * @description:
 * @projectName: SJGLSurfaceView
 * @date: 2018-04-21
 * @time: 13:21
 */
public class a {

    public final static void genChildTri(
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            int iteNum, List<Float> list) {

        if (list == null)
            return;

        if (0 == iteNum) {
            list.add(ax);
            list.add(ay);
            list.add(az);
            list.add(bx);
            list.add(by);
            list.add(bz);
            list.add(cx);
            list.add(cy);
            list.add(cz);
            return;
        }

        float aax = 0.5f * (bx + cx);
        float aay = 0.5f * (by + cy);
        float aaz = 0.5f * (bz + cz);

        float bbx = 0.5f * (ax + cx);
        float bby = 0.5f * (ay + cy);
        float bbz = 0.5f * (az + cz);

        float ccx = 0.5f * (bx + ax);
        float ccy = 0.5f * (by + ay);
        float ccz = 0.5f * (bz + az);

        // normlize
        float normaa = 1.f / ((float) Math.sqrt(aax * aax + aay * aay + aaz * aaz));
        aax *= normaa;
        aay *= normaa;
        aaz *= normaa;

        float normbb = 1.f / ((float) Math.sqrt(bbx * bbx + bby * bby + bbz * bbz));
        bbx *= normbb;
        bby *= normbb;
        bbz *= normbb;

        float normcc = 1.f / ((float) Math.sqrt(ccx * ccx + ccy * ccy + ccz * ccz));
        ccx *= normcc;
        ccy *= normcc;
        ccz *= normcc;

        genChildTri(
                aax, aay, aaz,
                bx, by, bz,
                ccx, ccy, ccz,
                iteNum - 1, list);

        genChildTri(
                ax, ay, az,
                bbx, bby, bbz,
                ccx, ccy, ccz,
                iteNum - 1, list);

        genChildTri(
                aax, aay, aaz,
                bbx, bby, bbz,
                cx, cy, cz,
                iteNum - 1, list);

        genChildTri(
                aax, aay, aaz,
                bbx, bby, bbz,
                ccx, ccy, ccz,
                iteNum - 1, list);
    }
}
