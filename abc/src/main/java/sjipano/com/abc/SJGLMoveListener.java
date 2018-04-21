package sjipano.com.abc;

/**
 * @author: yifan.lin
 * @description:
 * @projectName: SJGLSurfaceView
 * @date: 2018-04-21
 * @time: 13:51
 */
public interface SJGLMoveListener {
    void positionUpdate(float fov,
                        float yaw, float pitch,
                        float width, float height);

    void justClick();

    void saveXYZ();
}
