/**
 * @file App.jsx
 * @description Definición de rutas de la SPA y protección por permisos.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { Navigate, Route, Routes } from 'react-router-dom';
import Layout, { MENU } from './components/Layout';
import RutaProtegida from './components/RutaProtegida';
import { useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import TableroPage from './pages/TableroPage';
import ProductosPage from './pages/productos/ProductosPage';
import CatalogosPage from './pages/CatalogosPage';
import InventarioPage from './pages/inventario/InventarioPage';
import ReservasPage from './pages/inventario/ReservasPage';
import ComprasPage from './pages/compras/ComprasPage';
import NuevaCompraPage from './pages/compras/NuevaCompraPage';
import PuntoVentaPage from './pages/ventas/PuntoVentaPage';
import FacturasPage from './pages/ventas/FacturasPage';
import DocumentoImprimiblePage from './pages/DocumentoImprimiblePage';
import TercerosPage from './pages/TercerosPage';
import CierresPage from './pages/CierresPage';
import UsuariosPage from './pages/UsuariosPage';
import AuditoriaPage from './pages/AuditoriaPage';
import MigracionPage from './pages/MigracionPage';
import SincronizacionPage from './pages/SincronizacionPage';

const permisosDe = (ruta) => MENU.find((m) => m.ruta === ruta)?.permisos ?? [];

/**
 * Redirige al primer módulo permitido cuando el usuario no tiene acceso al tablero.
 * @returns {JSX.Element} tablero o redirección
 */
function Inicio() {
  const { tienePermiso } = useAuth();
  if (tienePermiso('REPORTE_LEER')) return <TableroPage />;
  const primero = MENU.find((m) => tienePermiso(...m.permisos));
  return <Navigate to={primero?.ruta ?? '/login'} replace />;
}

/**
 * @returns {JSX.Element} enrutador de la aplicación
 */
export default function App() {
  const p = (ruta, elemento) => <RutaProtegida permisos={permisosDe(ruta)}>{elemento}</RutaProtegida>;
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RutaProtegida><Layout /></RutaProtegida>}>
        <Route index element={<Inicio />} />
        <Route path="ventas" element={p('/ventas', <PuntoVentaPage />)} />
        <Route path="facturas" element={p('/facturas', <FacturasPage />)} />
        <Route path="compras" element={p('/compras', <ComprasPage />)} />
        <Route path="compras/nueva" element={<RutaProtegida permisos={['COMPRA_REGISTRAR']}><NuevaCompraPage /></RutaProtegida>} />
        <Route path="productos" element={p('/productos', <ProductosPage />)} />
        <Route path="catalogos" element={p('/catalogos', <CatalogosPage />)} />
        <Route path="inventario" element={p('/inventario', <InventarioPage />)} />
        <Route path="reservas" element={p('/reservas', <ReservasPage />)} />
        <Route path="terceros" element={p('/terceros', <TercerosPage />)} />
        <Route path="cierres" element={p('/cierres', <CierresPage />)} />
        <Route path="sincronizacion" element={p('/sincronizacion', <SincronizacionPage />)} />
        <Route path="migracion" element={p('/migracion', <MigracionPage />)} />
        <Route path="auditoria" element={p('/auditoria', <AuditoriaPage />)} />
        <Route path="usuarios" element={p('/usuarios', <UsuariosPage />)} />
        <Route path="imprimir/:tipo/:id" element={<DocumentoImprimiblePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
