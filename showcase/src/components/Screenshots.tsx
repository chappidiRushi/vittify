import { useState, useCallback, useEffect } from 'react'
import useEmblaCarousel from 'embla-carousel-react'
import { ChevronLeft, ChevronRight, X } from 'lucide-react'
import { screenshots } from '../data/screenshots'
import { motion, AnimatePresence } from 'framer-motion'

function Lightbox({
  index,
  onClose,
  onPrev,
  onNext,
}: {
  index: number
  onClose: () => void
  onPrev: () => void
  onNext: () => void
}) {
  const base = import.meta.env.BASE_URL
  const item = screenshots[index]

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
      if (e.key === 'ArrowLeft') onPrev()
      if (e.key === 'ArrowRight') onNext()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose, onPrev, onNext])

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.2 }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 backdrop-blur-xl p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label={`Screenshot: ${item.caption}`}
    >
      {/* Close */}
      <button
        className="absolute top-4 right-4 p-2 text-white/70 hover:text-white bg-white/10 hover:bg-white/20 rounded-full transition-all"
        onClick={onClose}
        aria-label="Close lightbox"
      >
        <X size={20} />
      </button>

      {/* Prev */}
      <button
        className="absolute left-4 p-3 text-white/70 hover:text-white bg-white/10 hover:bg-white/20 rounded-full transition-all"
        onClick={(e) => { e.stopPropagation(); onPrev() }}
        aria-label="Previous screenshot"
      >
        <ChevronLeft size={20} />
      </button>

      {/* Image */}
      <motion.div
        key={index}
        initial={{ scale: 0.92, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.25 }}
        className="relative max-w-xs w-full"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="rounded-[44px] overflow-hidden shadow-2xl border border-white/10 bg-[#0B0B14]">
          <img
            src={`${base}screenshots/${item.file}`}
            alt={item.caption}
            className="w-full h-auto"
          />
        </div>
        <p className="mt-4 text-center text-sm font-medium text-white/80">{item.caption}</p>
        <p className="text-center text-xs text-white/40 mt-1">{item.description}</p>
        <p className="text-center text-xs text-white/25 mt-2">{index + 1} / {screenshots.length}</p>
      </motion.div>

      {/* Next */}
      <button
        className="absolute right-4 p-3 text-white/70 hover:text-white bg-white/10 hover:bg-white/20 rounded-full transition-all"
        onClick={(e) => { e.stopPropagation(); onNext() }}
        aria-label="Next screenshot"
      >
        <ChevronRight size={20} />
      </button>
    </motion.div>
  )
}

export function Screenshots() {
  const [emblaRef, emblaApi] = useEmblaCarousel({
    loop: true,
    align: 'start',
    slidesToScroll: 1,
  })
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null)
  const base = import.meta.env.BASE_URL

  const onSelect = useCallback(() => {
    if (!emblaApi) return
    setSelectedIndex(emblaApi.selectedScrollSnap())
  }, [emblaApi])

  useEffect(() => {
    if (!emblaApi) return
    emblaApi.on('select', onSelect)
    onSelect()
  }, [emblaApi, onSelect])

  const scrollPrev = useCallback(() => emblaApi?.scrollPrev(), [emblaApi])
  const scrollNext = useCallback(() => emblaApi?.scrollNext(), [emblaApi])

  return (
    <section
      id="screenshots"
      className="py-28 bg-[var(--color-surface)] relative overflow-hidden"
    >
      {/* Background decoration */}
      <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-[var(--color-border)] to-transparent" />
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-[var(--color-border)] to-transparent" />

      <div className="max-w-6xl mx-auto px-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-6 mb-12">
          <div>
            <span className="text-xs font-semibold tracking-widest uppercase text-[var(--color-accent-lavender)]">
              Screenshots
            </span>
            <h2 className="mt-3 text-4xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
              See it in action.
            </h2>
            <p className="mt-2 text-[var(--color-text-secondary)]">
              Captured from Google Pixel 7 Pro running the Vittify beta.
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={scrollPrev}
              className="p-3 rounded-full bg-[var(--color-surface-high)] border border-[var(--color-border)] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface-highest)] transition-all"
              aria-label="Previous screenshot"
            >
              <ChevronLeft size={18} />
            </button>
            <button
              onClick={scrollNext}
              className="p-3 rounded-full bg-[var(--color-surface-high)] border border-[var(--color-border)] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface-highest)] transition-all"
              aria-label="Next screenshot"
            >
              <ChevronRight size={18} />
            </button>
          </div>
        </div>

        {/* Carousel */}
        <div className="overflow-hidden" ref={emblaRef}>
          <div className="flex gap-5 touch-pan-y">
            {screenshots.map((item, i) => (
              <div
                key={item.file}
                className="flex-none w-44 cursor-pointer group"
                onClick={() => setLightboxIndex(i)}
                role="button"
                tabIndex={0}
                aria-label={`Open ${item.caption} screenshot`}
                onKeyDown={(e) => e.key === 'Enter' && setLightboxIndex(i)}
              >
                <div
                  className={`relative w-full h-80 rounded-3xl overflow-hidden border transition-all duration-300 ${
                    selectedIndex === i
                      ? 'border-[var(--color-accent-lavender)]/40 shadow-lg shadow-[var(--color-accent-lavender)]/10'
                      : 'border-[var(--color-border)] opacity-70 group-hover:opacity-90'
                  }`}
                >
                  <img
                    src={`${base}screenshots/${item.file}`}
                    alt={item.caption}
                    className="w-full h-full object-cover object-top transition-transform duration-500 group-hover:scale-105"
                    loading="lazy"
                  />
                  {/* Hover overlay */}
                  <div className="absolute inset-0 bg-[var(--color-accent-lavender)]/0 group-hover:bg-[var(--color-accent-lavender)]/5 transition-all duration-300 flex items-end p-3">
                    <div className="opacity-0 group-hover:opacity-100 transition-opacity duration-300 text-white text-xs font-medium bg-black/60 backdrop-blur-sm rounded-lg px-2 py-1">
                      View
                    </div>
                  </div>
                </div>
                <div className="mt-3 px-1">
                  <p className="text-sm font-medium text-[var(--color-text-primary)] truncate">
                    {item.caption}
                  </p>
                  <p className="text-xs text-[var(--color-text-tertiary)] mt-0.5 truncate">
                    {item.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Dot indicators */}
        <div className="flex justify-center gap-1.5 mt-8">
          {screenshots.map((_, i) => (
            <button
              key={i}
              onClick={() => emblaApi?.scrollTo(i)}
              className="transition-all duration-300 h-1 rounded-full"
              style={{
                width: i === selectedIndex ? 24 : 6,
                backgroundColor:
                  i === selectedIndex
                    ? 'var(--color-accent-lavender)'
                    : 'var(--color-surface-highest)',
              }}
              aria-label={`Go to screenshot ${i + 1}`}
            />
          ))}
        </div>
      </div>

      {/* Lightbox */}
      <AnimatePresence>
        {lightboxIndex !== null && (
          <Lightbox
            index={lightboxIndex}
            onClose={() => setLightboxIndex(null)}
            onPrev={() => setLightboxIndex((i) => ((i ?? 0) - 1 + screenshots.length) % screenshots.length)}
            onNext={() => setLightboxIndex((i) => ((i ?? 0) + 1) % screenshots.length)}
          />
        )}
      </AnimatePresence>
    </section>
  )
}
