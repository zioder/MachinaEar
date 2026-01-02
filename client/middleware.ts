import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Public routes that don't require authentication
  const publicRoutes = ["/", "/auth/callback", "/offline"];

  // Allow access to public routes, static assets, and API routes
  if (
    publicRoutes.includes(pathname) ||
    pathname.startsWith("/_next") ||
    pathname.startsWith("/api") ||
    pathname.startsWith("/manifest.json") ||
    pathname.startsWith("/sw.js") ||
    pathname.startsWith("/workbox")
  ) {
    return NextResponse.next();
  }

  // Check for authentication cookie
  // NOTE: Disabled because cookies are set by iam.machinaear.me and can't be read
  // by middleware running on machinaear.me, even with Domain=.machinaear.me
  // Authentication is handled by API calls instead
  // const accessToken = request.cookies.get("access_token");

  // If no access token cookie exists, redirect to login
  // Protected routes: /home, /settings, etc.
  // DISABLED: Cross-domain cookies don't work with Next.js middleware
  // if (!accessToken) {
  //   const loginUrl = new URL("/login", request.url);
  //   // Add return URL to redirect back after login
  //   loginUrl.searchParams.set("returnUrl", pathname);
  //   return NextResponse.redirect(loginUrl);
  // }

  // Allow all requests through - authentication is checked by useAuth hook
  // Token exists, allow request to proceed
  // Note: Token validation still happens on the server-side via API calls
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
