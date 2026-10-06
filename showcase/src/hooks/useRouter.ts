import { useState, useEffect, useCallback } from 'react'

export type AppRoute = 'home' | 'privacy' | 'terms'

function getRouteFromLocation(): AppRoute {
  if (typeof window === 'undefined') return 'home'

  const path = window.location.pathname.toLowerCase().replace(/\/$/, '')
  const hash = window.location.hash.toLowerCase()

  if (
    path === '/privacy' || 
    path.endsWith('/privacy') ||
    hash === '#/privacy' || 
    hash === '#privacy-policy'
  ) {
    return 'privacy'
  }

  if (
    path === '/terms' || 
    path === '/terms-of-service' || 
    path === '/terms-of-use' ||
    path.endsWith('/terms') ||
    hash === '#/terms' || 
    hash === '#terms-of-service' ||
    hash === '#terms-of-use'
  ) {
    return 'terms'
  }

  return 'home'
}

export function useRouter() {
  const [route, setRoute] = useState<AppRoute>(getRouteFromLocation)

  useEffect(() => {
    const handleLocationChange = () => {
      const newRoute = getRouteFromLocation()
      setRoute(newRoute)
    }

    window.addEventListener('popstate', handleLocationChange)
    window.addEventListener('hashchange', handleLocationChange)

    return () => {
      window.removeEventListener('popstate', handleLocationChange)
      window.removeEventListener('hashchange', handleLocationChange)
    }
  }, [])

  const navigate = useCallback((target: AppRoute | string, targetHash?: string) => {
    let newRoute: AppRoute = 'home'
    let url = '/'

    if (target === 'privacy' || target === '/privacy') {
      newRoute = 'privacy'
      url = '/privacy'
    } else if (target === 'terms' || target === '/terms' || target === '/terms-of-service') {
      newRoute = 'terms'
      url = '/terms'
    } else {
      newRoute = 'home'
      url = targetHash ? `/${targetHash}` : '/'
    }

    if (window.location.pathname !== url) {
      window.history.pushState(null, '', url)
    }

    setRoute(newRoute)

    if (targetHash && newRoute === 'home') {
      setTimeout(() => {
        const el = document.querySelector(targetHash)
        if (el) {
          el.scrollIntoView({ behavior: 'smooth' })
        } else {
          window.scrollTo({ top: 0, behavior: 'smooth' })
        }
      }, 50)
    } else {
      window.scrollTo({ top: 0, behavior: 'instant' })
    }
  }, [])

  return { route, navigate }
}
