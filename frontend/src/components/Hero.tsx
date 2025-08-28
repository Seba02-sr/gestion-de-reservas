import { MapPinIcon, CalendarIcon, UserGroupIcon, MagnifyingGlassIcon } from "@heroicons/react/24/outline"

export default function Hero() {
  return (
    <section className="bg-gradient-to-br from-slate-50 to-slate-100 py-16 px-4">
      <div className="max-w-6xl mx-auto text-center">
        <h1 className="rarf-typography-h1 mb-4 text-slate-800">Encuentra tu hotel perfecto</h1>
        <p className="rarf-typography-p mb-12 text-slate-600 max-w-2xl mx-auto">
          Miles de hoteles disponibles al mejor precio
        </p>

        <div className="bg-white rounded-2xl shadow-lg p-8 max-w-4xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
            {/* Destino */}
            <div className="text-left">
              <label className="block rarf-typography-span text-slate-700 mb-2 rarn-font-weight-medium">Destino</label>
              <div className="relative">
                <MapPinIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-400" />
                <input
                  type="text"
                  placeholder="¿A dónde viajas?"
                  className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent rarf-typography-p"
                />
              </div>
            </div>

            {/* Check-in */}
            <div className="text-left">
              <label className="block rarf-typography-span text-slate-700 mb-2 rarn-font-weight-medium">Check-in</label>
              <div className="relative">
                <CalendarIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-400" />
                <input
                  type="date"
                  className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent rarf-typography-p"
                />
              </div>
            </div>

            {/* Check-out */}
            <div className="text-left">
              <label className="block rarf-typography-span text-slate-700 mb-2 rarn-font-weight-medium">
                Check-out
              </label>
              <div className="relative">
                <CalendarIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-400" />
                <input
                  type="date"
                  className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent rarf-typography-p"
                />
              </div>
            </div>

            {/* Huéspedes */}
            <div className="text-left">
              <label className="block rarf-typography-span text-slate-700 mb-2 rarn-font-weight-medium">
                Huéspedes
              </label>
              <div className="relative">
                <UserGroupIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-400" />
                <select className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent rarf-typography-p appearance-none bg-white">
                  <option>1 huésped</option>
                  <option>2 huéspedes</option>
                  <option>3 huéspedes</option>
                  <option>4 huéspedes</option>
                  <option>5+ huéspedes</option>
                </select>
              </div>
            </div>
          </div>

          <button className="w-full md:w-auto bg-slate-800 hover:bg-slate-700 text-white px-8 py-3 rounded-lg transition-colors duration-200 flex items-center justify-center gap-2 rarf-typography-p rarn-font-weight-medium">
            <MagnifyingGlassIcon className="h-5 w-5" />
            Buscar Hoteles
          </button>
        </div>
      </div>
    </section>
  )
}
