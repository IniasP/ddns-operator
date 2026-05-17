# DDNS Operator

Kubernetes operator to manage dynamic DNS using Cloudflare and deploy simple static sites behind the managed records.

## Assumptions

This demo project makes some assumptions.

- The cluster is running behind a single external IP. That is, the result of `curl -4 icanhazip.com` is the same on
  every node.
- [Cert-manager](https://cert-manager.io/docs/) is installed, and a `ClusterIssuer` (e.g. `letsencrypt-prod`) exists.

## Routing

The operator supports two mutually exclusive routing modes for `Site` resources. Configure one (or neither) per site.

### Ingress

Set `spec.ingress.enabled: true` on a `Site` to create a Kubernetes `Ingress` resource. The ingress class defaults
to `"nginx"` but can be overridden via `spec.ingress.ingressClassName`. Annotations (e.g. for cert-manager) can be
passed via `spec.ingress.annotations`.

### Gateway API (HTTPRoute)

Set `spec.httpRoute.enabled: true` on a `Site` to create a Gateway API `HTTPRoute` resource instead.

| Field | Required | Default | Description |
|---|---|---|---|
| `gatewayName` | No | `"nginx"` | Name of the `Gateway` to attach to |
| `gatewayNamespace` | No | *(none)* | Namespace of the `Gateway` (e.g. `nginx-gateway`). Should be set when the Gateway is in a different namespace than the Site. |
| `sectionName` | No | *(none)* | Name of the specific listener on the Gateway to attach to (e.g. `https-my-site`). Required for HTTPS — without it the route attaches to the first matching listener (typically HTTP). |
| `certIssuerRef` | No | *(none)* | When set, the operator automatically creates a cert-manager `Certificate` resource in `gatewayNamespace`. See [TLS / Certificate automation](#tls--certificate-automation) below. |
| `rules` | No | *(none)* | Custom `HTTPRouteRule` list. If omitted the route matches all traffic for the hostname. |

The hostname is automatically derived from the associated `CloudflareRecord` status.

> **Note:** Gateway API CRDs must be installed in the cluster. See the
> [Gateway API documentation](https://gateway-api.sigs.k8s.io/guides/) for installation instructions.

#### TLS / Certificate automation

When `spec.httpRoute.certIssuerRef` is set, the operator creates a cert-manager `Certificate` in `gatewayNamespace`
(the namespace where the `Gateway` reads TLS secrets from). This replaces the manual `Certificate` resource that
would otherwise be needed.

```yaml
spec:
  httpRoute:
    enabled: true
    gatewayName: main-gateway
    gatewayNamespace: nginx-gateway
    sectionName: https-my-site        # must match a listener on the Gateway
    certIssuerRef:
      name: letsencrypt-prod          # name of the ClusterIssuer or Issuer
      kind: ClusterIssuer             # defaults to ClusterIssuer
      # group defaults to cert-manager.io
```

The operator will create a `Certificate` equivalent to:

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: my-site-tls          # <siteName>-tls
  namespace: nginx-gateway   # gatewayNamespace
spec:
  secretName: my-site-tls
  dnsNames: [my-site.example.com]   # from CloudflareRecord status
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
    group: cert-manager.io
```

The `secretName` (`<siteName>-tls`) must match the `certificateRefs[].name` in the corresponding Gateway listener.

#### What still requires manual configuration

The `Gateway` listener itself must be added manually to the `Gateway` resource, since the `Gateway` is typically a
shared, centrally-managed resource. For each HTTPS site, add a listener like:

```yaml
- name: https-my-site
  hostname: my-site.example.com
  protocol: HTTPS
  port: 443
  allowedRoutes:
    namespaces:
      from: All
  tls:
    mode: Terminate
    certificateRefs:
      - name: my-site-tls   # must match <siteName>-tls
```

#### Comparison: Ingress vs Gateway API

| Concern | Ingress | Gateway API |
|---|---|---|
| Route resource | `Ingress` (auto-created) | `HTTPRoute` (auto-created) |
| TLS certificate | Via annotation on `Ingress` (cert-manager handles it) | `Certificate` auto-created by operator when `certIssuerRef` is set |
| Gateway listener | N/A | Must be added manually to the shared `Gateway` resource |
| Namespace of TLS secret | Same as `Ingress` | Must be in `gatewayNamespace` (where the `Gateway` reads secrets) |
