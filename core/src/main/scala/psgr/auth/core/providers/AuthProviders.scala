package psgr.auth.core.providers

import javax.inject.Inject

import psgr.auth.core.providers.facebook.FacebookProvider
import psgr.auth.core.providers.google.GoogleProvider
import psgr.auth.core.providers.vk.VkProvider

class AuthProviders @Inject() (
    facebookCheckingProvider: FacebookProvider,
    credentialsProvider:      CredentialsProvider,
    vkProvider:               VkProvider,
    googleProvider:           GoogleProvider
) {

  def providers: Set[Provider] = Set(facebookCheckingProvider, credentialsProvider, vkProvider, googleProvider)
}