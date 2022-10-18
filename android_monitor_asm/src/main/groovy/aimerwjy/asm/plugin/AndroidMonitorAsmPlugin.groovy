package aimerwjy.asm.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidMonitorAsmPlugin implements Plugin<Project> {

    void apply(Project project) {
        System.out.println("android monitor asm plugin start")

        def android = project.extensions.getByType(AppExtension)
        if (android == null) {
            return
        }
        android.registerTransform(new AndroidMonitorAsmThreadTransform())
    }

}