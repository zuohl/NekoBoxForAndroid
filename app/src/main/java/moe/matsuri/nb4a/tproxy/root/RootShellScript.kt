package moe.matsuri.nb4a.tproxy.root

import moe.matsuri.nb4a.tproxy.core.XrayFakeDnsIpv4Pool
import moe.matsuri.nb4a.tproxy.utils.shellQuote

fun StringBuilder.appendScript(script: String) {
    append(script.trimIndent())
    append('\n')
}

fun StringBuilder.appendHeredoc(
    targetPath: String,
    content: String,
) {
    appendScript("cat > ${targetPath.shellQuote()} <<'$BootScriptHeredocDelimiter'")
    append(content)
    if (!content.endsWith('\n')) {
        append('\n')
    }
    appendScript(BootScriptHeredocDelimiter)
}

fun StringBuilder.appendDeleteRuleLoop(
    command: String,
    chain: String,
    rule: String,
    table: String = "mangle",
) {
    appendScript("while $command -t $table -D $chain $rule 2>/dev/null; do :; done")
}

fun StringBuilder.appendIpRuleDeleteLoop(
    ipCommand: String,
    rule: String,
) {
    appendScript("while $ipCommand rule del $rule 2>/dev/null; do :; done")
}

fun StringBuilder.appendRootEbpfXtbpfMarkRules(
    command: String,
    chain: String,
    routeMark: String,
    ownerBypassGid: Int? = null,
) {
    val ownerMatch = ownerBypassGid?.let { gid -> "-m owner ! --gid-owner $gid " }.orEmpty()
    val quotedProgramPath = command.rootEbpfProgramPath(
        ipv4Path = RootEbpfXtOutputV4ProgramPath,
        ipv6Path = RootEbpfXtOutputV6ProgramPath,
    ).shellQuote()
    appendScript(
        """
        $command -t mangle -A $chain -p tcp ${ownerMatch}-m bpf --object-pinned $quotedProgramPath -j MARK --set-xmark $routeMark
        $command -t mangle -A $chain -p udp ${ownerMatch}-m bpf --object-pinned $quotedProgramPath -j MARK --set-xmark $routeMark
        """,
    )
}

fun StringBuilder.appendRootEbpfXtbpfInterfaceMarkRules(
    command: String,
    chain: String,
    routeMark: String,
    interfacePrefixes: List<String>,
) {
    val quotedProgramPath = command.rootEbpfProgramPath(
        ipv4Path = RootEbpfXtPreroutingV4ProgramPath,
        ipv6Path = RootEbpfXtPreroutingV6ProgramPath,
    ).shellQuote()
    interfacePrefixes.forEach { prefix ->
        val quotedInterface = prefix.shellQuote()
        appendScript(
            """
            $command -t mangle -A $chain -i $quotedInterface -p tcp -m bpf --object-pinned $quotedProgramPath -j MARK --set-xmark $routeMark
            $command -t mangle -A $chain -i $quotedInterface -p udp -m bpf --object-pinned $quotedProgramPath -j MARK --set-xmark $routeMark
            """,
        )
    }
}

fun StringBuilder.appendRootEbpfXtbpfInterfaceTproxyRules(
    command: String,
    chain: String,
    interfacePrefixes: List<String>,
    port: Int,
    onIp: String,
    mark: String,
) {
    val quotedProgramPath = command.rootEbpfProgramPath(
        ipv4Path = RootEbpfXtPreroutingV4ProgramPath,
        ipv6Path = RootEbpfXtPreroutingV6ProgramPath,
    ).shellQuote()
    interfacePrefixes.forEach { prefix ->
        val quotedInterface = prefix.shellQuote()
        appendScript(
            """
            $command -t mangle -A $chain -i $quotedInterface -p tcp -m bpf --object-pinned $quotedProgramPath -j TPROXY --on-port $port --on-ip $onIp --tproxy-mark $mark
            $command -t mangle -A $chain -i $quotedInterface -p udp -m bpf --object-pinned $quotedProgramPath -j TPROXY --on-port $port --on-ip $onIp --tproxy-mark $mark
            """,
        )
    }
}

private fun String.rootEbpfProgramPath(ipv4Path: String, ipv6Path: String): String {
    return if (this == RootIp6tablesCommand) ipv6Path else ipv4Path
}

fun StringBuilder.appendRootIpv6DnsRejectRules() {
    appendRootIpv6DnsRejectCleanupRules()
    appendScript(
        """
        $RootIp6tablesCommand -t mangle -I PREROUTING 1 -p udp --dport 53 -j DROP
        $RootIp6tablesCommand -t filter -I INPUT 1 -p udp --dport 53 -j REJECT
        $RootIp6tablesCommand -t filter -I FORWARD 1 -p udp --dport 53 -j REJECT
        $RootIp6tablesCommand -t filter -I OUTPUT 1 -p udp --dport 53 -j REJECT
        """,
    )
}

fun StringBuilder.appendRootIpv6DnsRejectCleanupRules() {
    appendDeleteRuleLoop(
        command = RootIp6tablesCommand,
        chain = "PREROUTING",
        rule = "-p udp --dport 53 -j DROP",
        table = "mangle",
    )
    appendDeleteRuleLoop(
        command = RootIp6tablesCommand,
        chain = "INPUT",
        rule = "-p udp --dport 53 -j REJECT",
        table = "filter",
    )
    appendDeleteRuleLoop(
        command = RootIp6tablesCommand,
        chain = "FORWARD",
        rule = "-p udp --dport 53 -j REJECT",
        table = "filter",
    )
    appendDeleteRuleLoop(
        command = RootIp6tablesCommand,
        chain = "OUTPUT",
        rule = "-p udp --dport 53 -j REJECT",
        table = "filter",
    )
}

fun StringBuilder.appendRootFakeDnsIcmpReplyRules() {
    appendRootFakeDnsIcmpReplyCleanupRules()
    val pool = XrayFakeDnsIpv4Pool.shellQuote()
    appendScript(
        """
        $RootIptablesCommand -t nat -N $RootFakeDnsIcmpReplyChain 2>/dev/null || true
        $RootIptablesCommand -t nat -N $RootFakeDnsIcmpReplyPreroutingChain 2>/dev/null || true
        $RootIptablesCommand -t nat -I OUTPUT 1 -j $RootFakeDnsIcmpReplyChain
        $RootIptablesCommand -t nat -I PREROUTING 1 -j $RootFakeDnsIcmpReplyPreroutingChain
        $RootIptablesCommand -t nat -A $RootFakeDnsIcmpReplyChain -d $pool -p icmp --icmp-type echo-request -j REDIRECT
        $RootIptablesCommand -t nat -A $RootFakeDnsIcmpReplyPreroutingChain -d $pool -p icmp --icmp-type echo-request -j REDIRECT
        """,
    )
}

fun StringBuilder.appendRootFakeDnsIcmpReplyCleanupRules() {
    appendDeleteRuleLoop(RootIptablesCommand, "OUTPUT", "-j $RootFakeDnsIcmpReplyChain", table = "nat")
    appendDeleteRuleLoop(RootIptablesCommand, "PREROUTING", "-j $RootFakeDnsIcmpReplyPreroutingChain", table = "nat")
    appendScript(
        """
        $RootIptablesCommand -t nat -F $RootFakeDnsIcmpReplyChain 2>/dev/null || true
        $RootIptablesCommand -t nat -X $RootFakeDnsIcmpReplyChain 2>/dev/null || true
        $RootIptablesCommand -t nat -F $RootFakeDnsIcmpReplyPreroutingChain 2>/dev/null || true
        $RootIptablesCommand -t nat -X $RootFakeDnsIcmpReplyPreroutingChain 2>/dev/null || true
        """,
    )
}
