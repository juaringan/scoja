import javax.net.ssl.*;

/**
 * Legal protocol names:
 *   Default, SSL, SSLv2, SSLv3, TLS, TLSv1, TLSv1.1
 * Sun provider allows the following protocol names:
 *   Default, SSL, SSLv3, TLS, TLSv1.
 * Neither SSLv2 nor TLSv1.1 are implemented.
 * All the protocol names produces the same kind off SSLContext, one
 * that provides the protocols SSLv2Hello, SSLv3, TLSv1.
 *
 * <p>
 * The Default SSLContext is initialized and init calls fail with an exception.
 * When getting the SSLContext with any other name,
 * the result is uninitialized;
 * some methods (getProtocol, getProvider) can be called on this object;
 * but most methods require a previous call to init.
 */
public class ContextDumper {

    public static void main(final String[] args)
    throws Exception {
        final String protoName = args[0];
        final SSLContext ctxt1 = SSLContext.getInstance(protoName);
        final SSLContext ctxt2 = SSLContext.getInstance(protoName);
        try {
            ctxt1.init(null, null, null);
        } catch (java.security.KeyManagementException ignored) {}
        System.err.println("Same context: " + (ctxt1 == ctxt2));
        System.err.println("Protocol: " + ctxt1.getProtocol());
        System.err.println("Provider: " + ctxt1.getProvider());
        System.err.println("Supported: "
                + toString(ctxt1.getSupportedSSLParameters()));
        System.err.println("Default: "
                + toString(ctxt1.getDefaultSSLParameters()));
    }
    
    public static String toString(final SSLParameters ps) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Want client auth: ").append(ps.getWantClientAuth())
            .append("\nNeed client auth: ").append(ps.getNeedClientAuth())
            .append("\nProtocols:");
        for (final String p: ps.getProtocols()) sb.append(' ').append(p);
        sb.append("\nCipher suites:");
        for (final String c: ps.getCipherSuites()) sb.append(' ').append(c);
        return sb.toString();
    }
}
