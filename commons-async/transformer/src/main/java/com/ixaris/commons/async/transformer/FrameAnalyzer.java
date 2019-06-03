/*
 * Copyright (C) 2015 Electronic Arts Inc. All rights reserved.
 * Copyright (C) 2017 Ixaris Systems Ltd. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of Electronic Arts, Inc. ("EA") nor the names of
 * its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ixaris.commons.async.transformer;

import static com.ixaris.commons.async.transformer.AsmHelper.ASM_VERSION;

import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.FrameNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicInterpreter;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.tree.analysis.Frame;
import jdk.internal.org.objectweb.asm.tree.analysis.Interpreter;

/**
 * uses previous frames consider uninitialized values
 */
final class FrameAnalyzer extends Analyzer<BasicValue> {
    
    static final class ExtendedValue extends BasicValue {
        
        AbstractInsnNode insnNode;
        boolean uninitialized;
        BasicValue[] undecided;
        
        private ExtendedValue(final Type type) {
            super(type);
        }
        
        boolean isUninitialized() {
            return uninitialized;
        }
        
        @Override
        public boolean equals(final Object value) {
            if (insnNode != null || uninitialized) {
                if (value instanceof ExtendedValue) {
                    final ExtendedValue extvalue = (ExtendedValue) value;
                    return (extvalue.uninitialized == uninitialized) && (extvalue.insnNode == insnNode);
                } else {
                    return false;
                }
            }
            return super.equals(value);
        }
        
        @Override
        public int hashCode() {
            return super.hashCode();
        }
        
        @Override
        public String toString() {
            return undecided != null ? "?" : uninitialized ? "%" + super.toString() : super.toString();
        }
        
    }
    
    static final class ExtendedFrame extends Frame<BasicValue> {
        
        static final BasicValue[] EMPTY_MONITORS = new BasicValue[0];
        
        boolean force;
        // treat this array as immutable
        BasicValue[] monitors = EMPTY_MONITORS;
        
        private ExtendedFrame(final int nLocals, final int nStack) {
            super(nLocals, nStack);
        }
        
        private ExtendedFrame(final Frame<? extends BasicValue> src) {
            super(src);
            if (src instanceof ExtendedFrame) {
                this.monitors = ((ExtendedFrame) src).monitors;
            }
        }
        
        @Override
        public Frame<BasicValue> init(final Frame<? extends BasicValue> src) {
            final Frame<BasicValue> frame = super.init(src);
            if ((frame instanceof ExtendedFrame) && (src instanceof ExtendedFrame)) {
                ((ExtendedFrame) frame).monitors = ((ExtendedFrame) src).monitors;
            }
            return frame;
        }
        
        @SuppressWarnings("squid:S1151")
        @Override
        public void execute(
            final AbstractInsnNode insn, final Interpreter<BasicValue> interpreter
        ) throws AnalyzerException {
            switch (insn.getOpcode()) {
                case MONITORENTER:
                    final BasicValue[] enterNewMonitors = new BasicValue[monitors.length + 1];
                    System.arraycopy(monitors, 0, enterNewMonitors, 0, monitors.length);
                    enterNewMonitors[monitors.length] = pop();
                    monitors = enterNewMonitors;
                    break;
                case MONITOREXIT:
                    // tracking the monitors by `Value` identity only works if
                    // all object values are always unique different even if they have the same type
                    // if that can't be guaranteed we could store each monitor in a local variable
                    
                    final BasicValue v = pop();
                    int iv = monitors.length;
                    // find lastIndexOf this monitor in the "monitor stack"
                    while (--iv >= 0 && monitors[iv] != v) ;
                    
                    // there are usually two or more MONITOREXIT for each monitor
                    // due to the compiler generated exception handler
                    // obs.: the application code won't be in the handlers' block
                    if (iv != -1) {
                        final BasicValue[] exitNewMonitors = new BasicValue[monitors.length - 1];
                        if (iv > 0) {
                            // if not the first element
                            System.arraycopy(monitors, 0, exitNewMonitors, 0, iv);
                        }
                        if (monitors.length - iv > 1) {
                            // if not the last element
                            System.arraycopy(monitors, iv + 1, exitNewMonitors, iv, monitors.length - iv);
                        }
                        monitors = exitNewMonitors;
                    }
                    break;
                case INVOKESPECIAL:
                    final MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
                    if (methodInsnNode.name.equals("<init>")) {
                        // clear uninitialized flag from values in the frame
                        final BasicValue target = getStack(
                            getStackSize() - (1 + Type.getArgumentTypes(methodInsnNode.desc).length)
                        );
                        final BasicValue newValue = interpreter.newValue(target.getType());
                        super.execute(insn, interpreter);
                        for (int i = 0; i < getLocals(); i++) {
                            if (target.equals(getLocal(i))) {
                                setLocal(i, newValue);
                            }
                        }
                        int s = getStackSize();
                        final BasicValue[] stack = new BasicValue[s];
                        for (int i = s; --i >= 0; ) {
                            final BasicValue vv = pop();
                            stack[i] = target.equals(vv) ? newValue : vv;
                        }
                        for (int i = 0; i < s; i++) {
                            push(stack[i]);
                        }
                        break;
                    }
                    // fallthrough
                default:
                    super.execute(insn, interpreter);
                    break;
            }
        }
        
        @Override
        public boolean merge(
            final Frame<? extends BasicValue> frame, final Interpreter<BasicValue> interpreter
        ) throws AnalyzerException {
            if (force) {
                // uses the current frame
                return true;
            }
            if ((frame instanceof ExtendedFrame) && ((ExtendedFrame) frame).force) {
                init(frame);
                return true;
            }
            return super.merge(frame, interpreter);
        }
        
    }
    
    /**
     * Used to discover the object types that are currently being stored in the stack and in the locals.
     */
    private static final BasicInterpreter TYPE_INTERPRETER = new BasicInterpreter(ASM_VERSION) {
        
        @Override
        public BasicValue newValue(Type type) {
            if ((type != null) && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)) {
                return new ExtendedValue(type);
            }
            return super.newValue(type);
        }
        
        @Override
        public BasicValue newOperation(final AbstractInsnNode insn) throws AnalyzerException {
            if (insn.getOpcode() == NEW) {
                final Type type = Type.getObjectType(((TypeInsnNode) insn).desc);
                final ExtendedValue extendedValue = new ExtendedValue(type);
                extendedValue.uninitialized = true;
                extendedValue.insnNode = insn;
                return extendedValue;
            }
            return super.newOperation(insn);
        }
        
        @Override
        public BasicValue merge(final BasicValue v, final BasicValue w) {
            if (v != w && !v.equals(w)) {
                final Type t = v.getType();
                final Type u = w.getType();
                if (t != null && u != null && t.getSort() == Type.OBJECT && u.getSort() == Type.OBJECT) {
                    // could find a common super type here, a bit expensive
                    // TODO: test this with an assignment
                    // like: local1 was CompletableFuture <- store Task
                    final ExtendedValue nv = (ExtendedValue) newValue(BasicValue.REFERENCE_VALUE.getType());
                    nv.undecided = new BasicValue[] { v, w };
                    return nv;
                }
            }
            return super.merge(v, w);
        }
        
    };
    
    FrameAnalyzer() {
        super(TYPE_INTERPRETER);
    }
    
    @Override
    protected ExtendedFrame newFrame(final int nLocals, final int nStack) {
        return new ExtendedFrame(nLocals, nStack);
    }
    
    @Override
    protected ExtendedFrame newFrame(final Frame<? extends BasicValue> src) {
        return new ExtendedFrame(src);
    }
    
    protected void init(final String owner, final MethodNode m) throws AnalyzerException {
        final Frame<BasicValue>[] frames = getFrames();
        
        // populates frames from frame nodes
        AbstractInsnNode insnNode = m.instructions.getFirst();
        Frame<BasicValue> lastFrame = frames[0];
        for (int insnIndex = 0; insnNode != null; insnNode = insnNode.getNext(), insnIndex++) {
            if (insnNode instanceof FrameNode) {
                final FrameNode frameNode = (FrameNode) insnNode;
                final int frameType = frameNode.type;
                if (frameType == F_NEW || frameType == F_FULL) {
                    final ExtendedFrame frame = newFrame(lastFrame);
                    frame.force = true;
                    frames[insnIndex] = frame;
                    int iLocal_w = 0;
                    if (frameNode.local != null && frameNode.local.size() > 0) {
                        for (int j = 0; j < frameNode.local.size(); j++) {
                            BasicValue value = convertFrameNodeType(frameNode.local.get(j));
                            frame.setLocal(iLocal_w, value);
                            iLocal_w += value.getSize();
                        }
                    }
                    final BasicValue nullValue = TYPE_INTERPRETER.newValue(null);
                    while (iLocal_w < m.maxLocals) {
                        frame.setLocal(iLocal_w++, nullValue);
                    }
                    frame.clearStack();
                    if (frameNode.stack != null && frameNode.stack.size() > 0) {
                        for (int j = 0; j < frameNode.stack.size(); j++) {
                            frame.push(convertFrameNodeType(frameNode.stack.get(j)));
                        }
                    }
                    lastFrame = frame;
                }
            }
        }
    }
    
    // converts FrameNode information to the way Frame stores it
    private BasicValue convertFrameNodeType(final Object v) throws AnalyzerException {
        if (v instanceof String) {
            return TYPE_INTERPRETER.newValue(Type.getObjectType((String) v));
        } else if (v instanceof Integer) {
            if (v.equals(Opcodes.TOP)) {
                // TODO: check this
                return TYPE_INTERPRETER.newValue(null);
            } else if (v.equals(Opcodes.INTEGER)) {
                return TYPE_INTERPRETER.newValue(Type.INT_TYPE);
            } else if (v.equals(Opcodes.FLOAT)) {
                return TYPE_INTERPRETER.newValue(Type.FLOAT_TYPE);
            } else if (v.equals(Opcodes.DOUBLE)) {
                return TYPE_INTERPRETER.newValue(Type.DOUBLE_TYPE);
            } else if (v.equals(Opcodes.LONG)) {
                return TYPE_INTERPRETER.newValue(Type.LONG_TYPE);
            } else if (v.equals(Opcodes.NULL)) {
                // TODO: check this
                return TYPE_INTERPRETER.newValue(BasicValue.REFERENCE_VALUE.getType());
            } else if (v.equals(Opcodes.UNINITIALIZED_THIS)) {
                // TODO: check this
                return TYPE_INTERPRETER.newValue(null);
            }
        } else if (v instanceof LabelNode) {
            AbstractInsnNode node = (AbstractInsnNode) v;
            while (node.getOpcode() != NEW) {
                node = node.getNext();
            }
            return TYPE_INTERPRETER.newOperation(node);
        }
        return TYPE_INTERPRETER.newValue(null);
    }
    
}
