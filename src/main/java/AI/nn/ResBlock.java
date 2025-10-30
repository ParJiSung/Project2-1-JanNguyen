package AI.nn;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Activation;
import ai.djl.nn.Block;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.norm.BatchNorm;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;

public final class ResBlock extends AbstractBlock{
    private static final byte VERSION = 1;
    private final Block conv1;
    private final Block bn1;
    private final Block conv2;
    private final Block bn2;

    public ResBlock(int num_hidden){
        super(VERSION);
        conv1 = addChildBlock(
                "conv1",
                Conv2d.builder()
                        .setFilters(num_hidden)
                        .setKernelShape(new Shape(3, 3))
                        .optPadding(new Shape(1, 1))
                        .build());
        bn1 = addChildBlock("bn1", BatchNorm.builder().build());
        conv2 = addChildBlock(
                "conv2",
                Conv2d.builder()
                        .setFilters(num_hidden)
                        .setKernelShape(new Shape(3, 3))
                        .optPadding(new Shape(1, 1))
                        .build());
        bn2 = addChildBlock("bn2", BatchNorm.builder().build());
    }

    @Override
    protected NDList forwardInternal(ParameterStore ps, NDList inputs, boolean training, PairList<String, Object> params) {
        NDArray x = inputs.head();
        NDArray residual = x;
        NDArray y = conv1.forward(ps, new NDList(x), training).head();
        y = bn1.forward(ps, new NDList(y), training).head();
        y = Activation.relu(y);
        y = conv2.forward(ps, new NDList(y), training).head();
        y = bn2.forward(ps, new NDList(y), training).head();
        y = y.add(residual);
        y = Activation.relu(y);
        return new NDList(y);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        return inputShapes; // same shape as input (channels unchanged)
    }

    @Override
    protected void initializeChildBlocks(
        NDManager manager, DataType dataType, Shape... inputShapes) {

        Shape in = inputShapes[0];

        // conv1
        conv1.initialize(manager, dataType, in);
        Shape s1 = conv1.getOutputShapes(new Shape[]{in})[0];

        // bn1
        bn1.initialize(manager, dataType, s1);
        Shape s1b = bn1.getOutputShapes(new Shape[]{s1})[0];

        // conv2
        conv2.initialize(manager, dataType, s1b);
        Shape s2 = conv2.getOutputShapes(new Shape[]{s1b})[0];

        // bn2
        bn2.initialize(manager, dataType, s2);
        // output shape == input shape (channels unchanged), no change needed
    }
}
