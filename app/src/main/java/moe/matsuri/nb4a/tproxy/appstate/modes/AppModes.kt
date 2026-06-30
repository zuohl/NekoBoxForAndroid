package moe.matsuri.nb4a.tproxy.appstate.modes

/** How the per-app proxy list is interpreted by the iptables/eBPF rules. */
const val ProxyAppListModeBlacklist = 0
const val ProxyAppListModeWhitelist = 1
const val ProxyAppListModeGlobal = 2