resolvers in ThisBuild += "lightbend-commercial-mvn" at
  "https://repo.lightbend.com/pass/bAjZCqToWiGGF1X0XwnguK_UzxU2c9ZR9Vp4muui4VH3ZVOU/commercial-releases"
resolvers in ThisBuild += Resolver.url("lightbend-commercial-ivy",
  url("https://repo.lightbend.com/pass/bAjZCqToWiGGF1X0XwnguK_UzxU2c9ZR9Vp4muui4VH3ZVOU/commercial-releases"))(Resolver.ivyStylePatterns)