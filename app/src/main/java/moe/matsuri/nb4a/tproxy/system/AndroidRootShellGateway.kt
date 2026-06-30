package moe.matsuri.nb4a.tproxy.system

class AndroidRootShellGateway {
    init {
        AndroidRootShell.configure()
    }

    suspend fun exec(command: String, options: ShellExecOptions = ShellExecOptions()): ShellExecResult {
        return AndroidRootShell.exec(command, options)
    }

    suspend fun hasRootAccess(): Boolean {
        return AndroidRootShell.hasRootAccess()
    }
}
