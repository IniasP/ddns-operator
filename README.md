# DDNS Operator

Kubernetes operator to manage dynamic DNS using Cloudflare and deploy simple static sites behind the managed records.

## Assumptions

This demo project makes some assumptions.

- The cluster is running behind a single external IP. That is, the result of `curl -4 icanhazip.com` is the same on
  every node.
- [Stakater Reloader](https://github.com/stakater/Reloader) is installed.
- Nginx is used for ingress. That is, `"nginx"` is a valid ingress class.
- [Cert-manager](https://cert-manager.io/docs/) is installed, and a cluster issuer named `"letsencrypt-prod"` exists.
