package aimerwjy.asm.plugin

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode


class AndroidMonitorAsmThreadTransform extends Transform {

    static THREAD = "java/lang/Thread"

    @Override
    String getName() {
        return "AndroidMonitorAsmThreadTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        //可以从中获取jar包和class文件夹路径。需要输出给下一个任务
        Collection<TransformInput> inputs = transformInvocation.getInputs()
        //OutputProvider管理输出路径
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
        //遍历所有的输入，有两种类型，分别是文件夹类型（也就是我们自己写的代码）和jar类型（引入的jar包），这里我们只处理自己写的代码。
        for (TransformInput input : inputs) {
            //遍历所有文件夹
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                //获取transform的输出目录，等我们插桩后就将修改过的class文件替换掉transform输出目录中的文件，就达到修改的效果了。
                System.out.println("transform=====" + directoryInput.getName())

                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY)
                transformDir(directoryInput.getFile(), dest)
            }
        }
    }

    /**
     * 遍历文件夹，对文件进行插桩
     *
     * @param input 源文件
     * @param dest 源文件修改后的输出地址
     */
    void transformDir(File input, File dest) throws IOException {
        if (dest.exists()) {
            FileUtils.forceDelete(dest);
        }
        System.out.println("transformDir=====" + input.getAbsolutePath())

        FileUtils.forceMkdir(dest);
        String srcDirPath = input.getAbsolutePath();
        String destDirPath = dest.getAbsolutePath();
        File[] fileList = input.listFiles();
        if (fileList == null) {
            return;
        }
        for (File file : fileList) {
            String destFilePath = file.getAbsolutePath().replace(srcDirPath, destDirPath);
            File destFile = new File(destFilePath)
            if (file.isDirectory()) {
                //如果是文件夹，继续遍历
                transformDir(file, destFile);
            } else if (file.isFile()) {
                //创造了大小为0的新文件，或者，如果该文件已存在，则将打开并删除该文件关闭而不修改，但更新文件日期和时间
                FileUtils.touch(destFile);
                asmHandleFile(file.getAbsolutePath(), destFile.getAbsolutePath());
            }
        }
    }

    /**
     * 通过ASM进行插桩
     *
     * @param inputPath 源文件路径
     * @param destPath 输出路径
     */
    void asmHandleFile(String inputPath, String destPath) {
        try {
            //获取源文件的输入流
            FileInputStream is = new FileInputStream(inputPath);
            //将原文件的输入流交给ASM的ClassReader

            ClassReader classReader = new ClassReader(is)
            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            classReader.accept(classNode, 0)

            classNode.methods?.forEach { methodNode ->
                methodNode.instructions.each {
                    // 如果是构造函数才继续进行
                    if (it.opcode == Opcodes.INVOKESPECIAL) {
                        transformInvokeSpecial((MethodInsnNode) it, klass, methodNode)
                    }
                }
            }

            //将文件保存到输出目录下
            FileOutputStream fos = new FileOutputStream(destPath);
            fos.write(classReader.toByteArray());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void transformInvokeSpecial(MethodInsnNode node, ClassNode klass, MethodNode method) {
        // 如果不是构造函数，就直接退出
        if (node.name != "<init>" || node.owner != THREAD) {
            return
        }
        println("transformInvokeSpecial")
        transformThreadInvokeSpecial(node, klass, method)
    }

    void transformThreadInvokeSpecial(MethodInsnNode node, ClassNode klass, MethodNode method) {
        switch (node.desc) {
        // Thread()
            case "()V":
                // Thread(Runnable)
            case "(Ljava/lang/Runnable;)V":
                method.instructions.insertBefore(
                        node,
                        new LdcInsnNode(klass.name)
                )
                def r = node.desc.lastIndexOf(')')
                def desc =
                        "${node.desc.substring(0, r)}Ljava/lang/String;${node.desc.substring(r)}"
                // println(" + $SHADOW_THREAD.makeThreadName(Ljava/lang/String;Ljava/lang/String;) => ${this.owner}.${this.name}${this.desc}: ${klass.name}.${method.name}${method.desc}")
                println(" * ${node.owner}.${node.name}${node.desc} => ${node.owner}.${node.name}$desc: ${klass.name}.${method.name}${method.desc}")
                node.desc = desc
                break
        }
    }

}