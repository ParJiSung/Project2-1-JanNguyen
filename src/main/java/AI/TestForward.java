package AI;

import AI.nn.ResNet;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;

public class TestForward {
    public static void main(String[] args) {
        int n = 11;
        int actionSize = n * n;
        Block net = new ResNet(n, n, 2, 32, actionSize);

        try (NDManager manager = NDManager.newBaseManager();
             Model model = Model.newInstance("hex-resnet")) {

            model.setBlock(net);

            Shape inputShape = new Shape(1, 3, n, n);
            net.initialize(manager, DataType.FLOAT32, new Shape(1, 3, n, n));  // <-- fix

            NDArray x = manager.randomUniform(0f, 1f, inputShape);
            NDList out = net.forward(new ParameterStore(manager, false), new NDList(x), false);

            System.out.println("policy shape: " + out.get(0).getShape());
            System.out.println("value  shape: " + out.get(1).getShape());
        }
    }
}
