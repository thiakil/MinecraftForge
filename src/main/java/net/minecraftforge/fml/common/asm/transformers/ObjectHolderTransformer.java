package net.minecraftforge.fml.common.asm.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

/**
 * Transformer that will
 * A) remove `final` attribute from @ObjectHolder annotated fields
 * B) remove `final` and add @DeFinaledHolderField to fields in @ObjectHolder annotated classes that do not have explicit holders
 *
 * The purpose of which is to remove reflective hacking of the Field reflection object modifiers field.
 */
public class ObjectHolderTransformer implements IClassTransformer
{
    /** classes that will have their public static final fields processed as if they had the annotation **/
    static final String[] BUILTIN_OBJECT_HOLDER_CLASSES = {
            "net.minecraft.init.Blocks",
            "net.minecraft.init.Items",
            "net.minecraft.init.MobEffects",
            "net.minecraft.init.Biomes",
            "net.minecraft.init.Enchantments",
            "net.minecraft.init.SoundEvents",
            "net.minecraft.init.PotionTypes",
    };
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (basicClass == null)
            return null;
        ClassReader cr = new ClassReader(basicClass);
        CV cv = new CV(ArrayUtils.contains(BUILTIN_OBJECT_HOLDER_CLASSES, transformedName));
        cr.accept(cv, 0);
        return cv.toByteArray();
    }

    private static class CV extends ClassVisitor
    {
        static final String OBJECTHOLDER_DESC = "Lnet/minecraftforge/fml/common/registry/GameRegistry$ObjectHolder;";
        static final String AUTO_OBJECTHOLDER_DESC = "Lnet/minecraftforge/registries/ObjectHolderRegistry$DeFinaledHolderField;";

        boolean isObjectHolderClass;

        CV(boolean isBuiltInObjectHolder)
        {
            super(Opcodes.ASM5, new ClassWriter(0));
            this.isObjectHolderClass = isBuiltInObjectHolder;
        }
    
        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible)
        {
            if (desc.equals(OBJECTHOLDER_DESC))
            {
                isObjectHolderClass = true;
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public FieldVisitor visitField(int access_visitField, String name, String desc, String signature, Object value)
        {
            return new FV(access_visitField, name, desc, signature, value);
        }

        public byte[] toByteArray(){
            return ((ClassWriter)cv).toByteArray();
        }

        private class FV extends FieldNode
        {
            private boolean hasDirectObjectHolder = false;

            FV(int access, String name, String desc, String signature, Object value)
            {
                super(Opcodes.ASM5, access, name, desc, signature, value);
            }

            @Override
            public void visitEnd()
            {
                super.visitEnd();
                if (isStaticFinal(this.access))
                {
                    if (hasDirectObjectHolder)
                    {
                        this.access &= ~Opcodes.ACC_FINAL;
                    }
                    else if (CV.this.isObjectHolderClass && (this.access & Opcodes.ACC_PUBLIC) != 0)
                    {
                        this.access &= ~Opcodes.ACC_FINAL;
                        //add the auto annotation
                        this.visitAnnotation(AUTO_OBJECTHOLDER_DESC, true).visitEnd();
                    }
                }
                this.accept(CV.this.cv);//send it to the ClassWriter
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible)
            {
                if (desc.equals(OBJECTHOLDER_DESC)){
                    hasDirectObjectHolder = true;
                }
                return super.visitAnnotation(desc, visible);
            }
        }
    }

    static final int StatFin = Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
    static boolean isStaticFinal(int access){
        return (access & StatFin) == StatFin;
    }
}
