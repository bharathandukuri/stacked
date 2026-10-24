import Cookies from "js-cookie"

export const ACCESS_TOKEN_COOKIE_NAME = "accessToken"

/**
 * Parses JWT expiration time (in milliseconds) safely without external dependencies.
 */
export function getTokenExpirationMs(token: string): number | null {
  try {
    const parts = token.split(".")
    if (parts.length < 2) return null
    // Base64Url decode payload
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/")
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    )
    const decoded = JSON.parse(jsonPayload)
    if (typeof decoded.exp === "number") {
      return decoded.exp * 1000
    }
    return null
  } catch {
    return null
  }
}

/**
 * Checks whether a JWT token is expired or within the safety buffer window.
 * Default buffer is 30 seconds to prevent using a token that might expire mid-flight.
 */
export function isTokenExpired(token: string, bufferSeconds: number = 30): boolean {
  const expMs = getTokenExpirationMs(token)
  if (!expMs) return true
  return Date.now() + bufferSeconds * 1000 >= expMs
}

/**
 * Sets the access token in a browser cookie.
 * @param token The JWT access token.
 * @param expiresInSeconds Optional lifetime in seconds (defaults to JWT exp claim).
 */
export function setAccessTokenCookie(token: string, expiresInSeconds?: number): void {
  if (!token) return

  let expires: number | Date = 7 // Default 7-day fallback

  if (expiresInSeconds && expiresInSeconds > 0) {
    // js-cookie interprets numeric expires as days, so convert seconds to day fraction
    expires = expiresInSeconds / 86400
  } else {
    const expMs = getTokenExpirationMs(token)
    if (expMs) {
      expires = new Date(expMs)
    }
  }

  Cookies.set(ACCESS_TOKEN_COOKIE_NAME, token, {
    expires,
    path: "/",
    sameSite: "lax",
    secure: typeof window !== "undefined" && window.location.protocol === "https:",
  })
}

/**
 * Retrieves the access token from cookies if valid and not expired.
 * Automatically purges the cookie if the token is already expired.
 */
export function getAccessTokenCookie(): string | null {
  const token = Cookies.get(ACCESS_TOKEN_COOKIE_NAME)
  if (!token) return null

  if (isTokenExpired(token)) {
    removeAccessTokenCookie()
    return null
  }

  return token
}

/**
 * Removes the access token cookie.
 */
export function removeAccessTokenCookie(): void {
  Cookies.remove(ACCESS_TOKEN_COOKIE_NAME, { path: "/" })
}

/**
 * React hook for access token cookie operations.
 */
export function useAuthCookie() {
  return {
    getAccessToken: getAccessTokenCookie,
    setAccessToken: setAccessTokenCookie,
    removeAccessToken: removeAccessTokenCookie,
    isExpired: isTokenExpired,
  }
}

