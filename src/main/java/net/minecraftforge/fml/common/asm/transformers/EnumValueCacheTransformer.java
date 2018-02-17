package net.minecraftforge.fml.common.asm.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Created by Thiakil on 16/02/2018.
 */
public class EnumValueCacheTransformer implements IClassTransformer
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

        public ClazzVisitor(ClassVisitor cv)
        {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            return new MethVis(super.visitMethod(access, name, desc, signature, exceptions));
        }
    }

    private static class MethVis extends MethodVisitor
    {

        public MethVis(MethodVisitor mv)
        {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
        {
            if (opcode == Opcodes.INVOKESTATIC && owner.equals("java/lang/Enum") && name.equals("valueOf")){
                super.visitMethodInsn(opcode, "net/minecraftforge/common/util/EnumHelper", name, desc, itf);
                return;
            }
            else if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals("java/lang/Class") && name.equals("getEnumConstants"))
            {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/common/util/EnumHelper", name, "(Ljava/lang/Class;)[Ljava/lang/Object;", false);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
