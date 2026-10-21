/**
 * @file components/Layout.jsx
 * @description Estructura principal: barra superior (conexión, idioma, usuario) y menú lateral filtrado por permisos RBAC.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  AppBar, Badge, Box, Chip, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText,
  MenuItem, Select, Toolbar, Tooltip, Typography, useMediaQuery,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import InventoryIcon from '@mui/icons-material/Inventory2';
import CategoryIcon from '@mui/icons-material/Category';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import BookmarkIcon from '@mui/icons-material/BookmarkAdded';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import ReceiptIcon from '@mui/icons-material/ReceiptLong';
import PeopleIcon from '@mui/icons-material/People';
import EventIcon from '@mui/icons-material/EventAvailable';
import SecurityIcon from '@mui/icons-material/AdminPanelSettings';
import HistoryIcon from '@mui/icons-material/ManageSearch';
import UploadIcon from '@mui/icons-material/UploadFile';
import SyncIcon from '@mui/icons-material/Sync';
import LogoutIcon from '@mui/icons-material/Logout';
import CloudOffIcon from '@mui/icons-material/CloudOff';
import CloudDoneIcon from '@mui/icons-material/CloudDone';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { useNotificacion } from '../context/NotificacionContext';
import useConexion from '../hooks/useConexion';

const ANCHO = 250;

/** Opciones del menú con el permiso requerido para verlas. */
export const MENU = [
  { ruta: '/', clave: 'tablero', icono: <DashboardIcon />, permisos: ['REPORTE_LEER'] },
  { ruta: '/ventas', clave: 'ventas', icono: <PointOfSaleIcon />, permisos: ['VENTA_REGISTRAR'] },
  { ruta: '/facturas', clave: 'facturas', icono: <ReceiptIcon />, permisos: ['VENTA_LEER'] },
  { ruta: '/compras', clave: 'compras', icono: <LocalShippingIcon />, permisos: ['COMPRA_LEER', 'COMPRA_REGISTRAR'] },
  { ruta: '/productos', clave: 'productos', icono: <InventoryIcon />, permisos: ['PRODUCTO_LEER'] },
  { ruta: '/catalogos', clave: 'catalogos', icono: <CategoryIcon />, permisos: ['CATALOGO_ESCRIBIR'] },
  { ruta: '/inventario', clave: 'inventario', icono: <WarehouseIcon />, permisos: ['INVENTARIO_LEER'] },
  { ruta: '/reservas', clave: 'reservas', icono: <BookmarkIcon />, permisos: ['RESERVA_GESTIONAR'] },
  { ruta: '/terceros', clave: 'terceros', icono: <PeopleIcon />, permisos: ['TERCERO_LEER'] },
  { ruta: '/cierres', clave: 'cierres', icono: <EventIcon />, permisos: ['CIERRE_LEER'] },
  { ruta: '/sincronizacion', clave: 'sincronizacion', icono: <SyncIcon />, permisos: ['SINCRONIZACION_EJECUTAR', 'SINCRONIZACION_RESOLVER'] },
  { ruta: '/migracion', clave: 'migracion', icono: <UploadIcon />, permisos: ['MIGRACION_EJECUTAR'] },
  { ruta: '/auditoria', clave: 'auditoria', icono: <HistoryIcon />, permisos: ['AUDITORIA_LEER'] },
  { ruta: '/usuarios', clave: 'usuarios', icono: <SecurityIcon />, permisos: ['USUARIO_GESTIONAR'] },
];

/**
 * @returns {JSX.Element} layout con menú y contenido
 */
export default function Layout() {
  const { t, i18n } = useTranslation();
  const { usuario, cerrarSesion, tienePermiso } = useAuth();
  const { notificar } = useNotificacion();
  const navigate = useNavigate();
  const movil = useMediaQuery('(max-width:900px)');
  const [abierto, setAbierto] = useState(false);

  const alSincronizar = useCallback(
    (r) => notificar(t('conexion.sincronizado', r), r.conflictos ? 'warning' : 'success'),
    [notificar, t],
  );
  const { enLinea, pendientes } = useConexion(alSincronizar);

  const menu = (
    <Box sx={{ width: ANCHO }} role="navigation">
      <Toolbar>
        <Typography variant="h6" fontWeight={700} color="primary">{t('app.nombre')}</Typography>
      </Toolbar>
      <Divider />
      <List dense>
        {MENU.filter((m) => tienePermiso(...m.permisos)).map((m) => (
          <ListItemButton
            key={m.ruta}
            component={NavLink}
            to={m.ruta}
            end={m.ruta === '/'}
            onClick={() => setAbierto(false)}
            sx={{ '&.active': { bgcolor: 'action.selected', fontWeight: 600 } }}
            data-testid={`menu-${m.clave}`}
          >
            <ListItemIcon>{m.icono}</ListItemIcon>
            <ListItemText primary={t(`menu.${m.clave}`)} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (th) => th.zIndex.drawer + 1 }} className="no-imprimir">
        <Toolbar sx={{ gap: 1 }}>
          {movil && (
            <IconButton color="inherit" onClick={() => setAbierto(!abierto)} aria-label="menu"><MenuIcon /></IconButton>
          )}
          <Typography variant="subtitle1" sx={{ flexGrow: 1 }} noWrap>{t('app.subtitulo')}</Typography>
          <Tooltip title={pendientes ? t('conexion.pendientes', { n: pendientes }) : ''}>
            <Badge badgeContent={pendientes} color="warning">
              <Chip
                size="small"
                icon={enLinea ? <CloudDoneIcon /> : <CloudOffIcon />}
                label={enLinea ? t('conexion.enLinea') : t('conexion.sinConexion')}
                color={enLinea ? 'success' : 'error'}
                data-testid="estado-conexion"
              />
            </Badge>
          </Tooltip>
          <Select
            size="small"
            value={i18n.language?.startsWith('en') ? 'en' : 'es'}
            onChange={(e) => i18n.changeLanguage(e.target.value)}
            sx={{ color: 'inherit', '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,.5)' } }}
            inputProps={{ 'aria-label': t('comun.idioma') }}
          >
            <MenuItem value="es">ES</MenuItem>
            <MenuItem value="en">EN</MenuItem>
          </Select>
          <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' } }}>{usuario?.nombreCompleto}</Typography>
          <Tooltip title={t('comun.salir')}>
            <IconButton color="inherit" onClick={() => { cerrarSesion(); navigate('/login'); }} data-testid="salir">
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Drawer
        variant={movil ? 'temporary' : 'permanent'}
        open={movil ? abierto : true}
        onClose={() => setAbierto(false)}
        sx={{ width: ANCHO, flexShrink: 0, '& .MuiDrawer-paper': { width: ANCHO, boxSizing: 'border-box' } }}
        className="no-imprimir"
      >
        {menu}
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: { xs: 1.5, md: 3 }, minWidth: 0 }}>
        <Toolbar className="no-imprimir" />
        <Outlet />
      </Box>
    </Box>
  );
}
