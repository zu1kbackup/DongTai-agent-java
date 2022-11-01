package io.dongtai.iast.core.bytecode.enhance.plugin.core.adapter;

import io.dongtai.iast.core.bytecode.enhance.ClassContext;
import io.dongtai.iast.core.bytecode.enhance.plugin.AbstractAdviceAdapter;
import io.dongtai.iast.core.handler.hookpoint.controller.HookType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * @author dongzhiyong@huoxian.cn
 */
public class SinkAdviceAdapter extends AbstractAdviceAdapter {
    public SinkAdviceAdapter(MethodVisitor mv, int access, String name, String desc, ClassContext context,
                             String framework, String signCode, boolean overpower) {
        super(mv, access, name, desc, context, framework, signCode);
    }

    @Override
    protected void before() {
        mark(tryLabel);
        Label elseLabel = new Label();
        enterSink();
        isTopLevelSink();
        mv.visitJumpInsn(EQ, elseLabel);
        captureMethodState(-1, HookType.SINK.getValue(), false);
        mark(elseLabel);
    }

    @Override
    protected void after(final int opcode) {
        leaveSink();
    }

/*    *//**
     * 方法结束前，如何判断是否需要throw、return，解决堆栈未对齐
     *
     * @param maxStack
     * @param maxLocals
     *//*
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mark(catchLabel);
        visitTryCatchBlock(tryLabel, catchLabel, mark(), ASM_TYPE_THROWABLE.getInternalName());
        Label elseLabel2 = new Label();
        isNotRequestReplay();
        mv.visitJumpInsn(EQ, elseLabel2);
        Label returnLabel = new Label();
        throwException();
        mark(returnLabel);
        mark(elseLabel2);
        after(ATHROW);
        if (mv != null) {
            mv.visitMaxs(maxStack, maxLocals);
        }
    }*/

    /**
     * 进入sink方法的字节码
     */
    private void enterSink() {
        invokeStatic(ASM_TYPE_SPY_HANDLER, SPY_HANDLER$getDispatcher);
        invokeInterface(ASM_TYPE_SPY_DISPATCHER, SPY$enterSink);
    }

    /**
     * 判断是否位于顶级sink方法的字节码
     */
    private void isTopLevelSink() {
        invokeStatic(ASM_TYPE_SPY_HANDLER, SPY_HANDLER$getDispatcher);
        invokeInterface(ASM_TYPE_SPY_DISPATCHER, SPY$isFirstLevelSink);
    }

    /**
     * 离开sink方法的字节码
     */
    private void leaveSink() {
        invokeStatic(ASM_TYPE_SPY_HANDLER, SPY_HANDLER$getDispatcher);
        invokeInterface(ASM_TYPE_SPY_DISPATCHER, SPY$leaveSink);
    }
}
