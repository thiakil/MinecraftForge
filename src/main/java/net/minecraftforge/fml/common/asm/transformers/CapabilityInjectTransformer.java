package net.minecraftforge.fml.common.asm.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

/**
 * De finals @CapabilityInject fields
 */
public class CapabilityInjectTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (basicClass == null)
            return null;
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClazzVisitor(cw), 0);
        return cw.toByteArray();
    }

    private static class ClazzVisitor extends ClassVisitor
    {
        ClassVisitor superProxyClassVisitor = new ClassVisitor(Opcodes.ASM5)//FieldNode.accept only takes a ClassVisitor, define a proxy here to allow it to pass to the ClassWriter.
        {
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
            {
                return ClazzVisitor.this.cv.visitField(access, name, desc, signature, value);
            }
        };

        ClazzVisitor(ClassVisitor cv)
        {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
        {
            return new FV(access, name, desc, signature, value);
        }

        private class FV extends FieldNode
        {
            FV(int access, String name, String desc, String signature, Object value)
            {
                super(Opcodes.ASM5, access, name, desc, signature, value);
            }

            @Override
            public void visitEnd()
            {
                super.visitEnd();
                this.accept(ClazzVisitor.this.superProxyClassVisitor);//send it to the ClassWriter
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible)
            {
                if (desc.equals("Lnet/minecraftforge/common/capabilities/CapabilityInject;")){
                    this.access &= ~Opcodes.ACC_FINAL;
                }
                return super.visitAnnotation(desc, visible);
            }
        }
    }
}
