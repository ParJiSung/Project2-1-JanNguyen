package AI.nn;

import java.util.ArrayList;
import java.util.List;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Activation;
import ai.djl.nn.Block;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.core.Linear;
import ai.djl.nn.norm.BatchNorm;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;

public final class ResNet extends AbstractBlock  {
    private static final byte VERSION = 1;
    private final Block start_block;
    private final List<ResBlock> backbone;
    private final Block policy_head;
    private final Block value_head;
    private final int rows;
    private final int cols;
    private final int action_size;

    public ResNet(int rows, int cols, int numResBlocks, int numHidden, int actionSize) {
        super(VERSION);
        this.rows = rows;
        this.cols = cols;
        this.action_size = actionSize;

        // startBlock: Conv(3 -> numHidden) 3x3 pad1 + BN + ReLU
        start_block = addChildBlock("start",
                new SequentialBlock()
                        .add(Conv2d.builder()
                                .setFilters(numHidden)
                                .setKernelShape(new Shape(3, 3))
                                .optPadding(new Shape(1, 1))
                                .optBias(true)
                                .build())
                        .add(BatchNorm.builder().build())
                        .add(Activation.reluBlock())
        );

        // backbone residual blocks
        backbone = new ArrayList<>();
        for (int i = 0; i < numResBlocks; i++) {
            ResBlock rb = new ResBlock(numHidden);
            backbone.add(rb);
            addChildBlock("resblock_" + i, rb);
        }

        // policy head
        policy_head = addChildBlock("policyHead",
                new SequentialBlock()
                        .add(Conv2d.builder()
                                .setFilters(32)
                                .setKernelShape(new Shape(3, 3))
                                .optPadding(new Shape(1, 1))
                                .optBias(true)
                                .build())
                        .add(BatchNorm.builder().build())
                        .add(Activation.reluBlock())
                        .add(ai.djl.nn.Blocks.batchFlattenBlock())     // flatten NCHW -> N*(C*H*W)
                        .add(Linear.builder().setUnits(actionSize).build())
        );

        // value head
        value_head = addChildBlock("valueHead",
                new SequentialBlock()
                        .add(Conv2d.builder()
                                .setFilters(3)
                                .setKernelShape(new Shape(3, 3))
                                .optPadding(new Shape(1, 1))
                                .optBias(true)
                                .build())
                        .add(BatchNorm.builder().build())
                        .add(Activation.reluBlock())
                        .add(ai.djl.nn.Blocks.batchFlattenBlock())
                        .add(Linear.builder().setUnits(1).build())
                        .add(Activation.tanhBlock())                   // output in [-1, 1]
        );
    }

    /** Forward: returns NDList(policyLogits, value). */
    @Override
    protected NDList forwardInternal(
            ParameterStore ps, NDList inputs, boolean training, PairList<String, Object> params) {

        NDArray x = inputs.head();    // (N, 3, rows, cols)

        x = start_block.forward(ps, new NDList(x), training).head();
        for (ResBlock rb : backbone) {
            x = rb.forward(ps, new NDList(x), training).head();
        }

        NDArray policy = policy_head.forward(ps, new NDList(x), training).head();  // (N, actionSize)
        NDArray value  = value_head.forward(ps, new NDList(x), training).head();   // (N, 1)

        return new NDList(policy, value);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        // input expected: (N, 3, rows, cols)
        return new Shape[] {
                new Shape(inputShapes[0].get(0), action_size),  // policy
                new Shape(inputShapes[0].get(0), 1)            // value
        };
    }

    /** Convenience: create an input shape for this model. */
    public Shape inputShape(long batchSize) {
        return new Shape(batchSize, 3, rows, cols);
    }

    @Override
        protected void initializeChildBlocks(
                NDManager manager, DataType dataType, Shape... inputShapes) {

        Shape in = inputShapes[0];                 // (N, C=3, H, W)

        // start block
        start_block.initialize(manager, dataType, in);
        Shape cur = start_block.getOutputShapes(new Shape[]{in})[0];

        // backbone res blocks
        for (ResBlock rb : backbone) {
                rb.initialize(manager, dataType, cur);
                cur = rb.getOutputShapes(new Shape[]{cur})[0];
        }

        // heads (both take the same feature map shape)
        policy_head.initialize(manager, dataType, cur);
        value_head.initialize(manager, dataType, cur);
        }
}
