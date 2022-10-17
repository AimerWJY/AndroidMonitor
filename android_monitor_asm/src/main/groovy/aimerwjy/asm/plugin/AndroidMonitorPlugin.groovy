package aimerwjy.asm.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidMonitorPlugin implements Plugin<Project>{

    void apply(Project project){
        System.out.println("app plugin ")
    }

}