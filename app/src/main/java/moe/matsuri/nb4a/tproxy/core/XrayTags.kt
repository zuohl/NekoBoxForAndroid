package moe.matsuri.nb4a.tproxy.core

/**
 * Inbound/outbound tag constants. Only the subset used by the tproxy root
 * engine is retained from the AsteriskNG port. sing-box inbound tags must
 * match the tags emitted by NekoBox's ConfigBuilder for the tproxy mode.
 */
object XrayTags {
    const val TPROXY_INBOUND = "tproxy-in"
    const val TPROXY_HTTP_INBOUND = "tproxy-http-in"
    const val LOCAL_SOCKS_INBOUND = "mixed-in"
}