import { useState } from 'react';
import { NavLink, Outlet, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function AppLayout() {
  const { user, isAdmin, logout } = useAuth();
  const { id } = useParams();
  const [menuOpen, setMenuOpen] = useState(false);

  const displayName =
    [user?.firstname, user?.lastname].filter(Boolean).join(' ') || user?.username || 'Usuário';

  const closeMenu = () => setMenuOpen(false);

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`} aria-label="Navegação principal">
        <div className="sidebar-brand">
          <strong>Periscope</strong>
          <span>Análise de patentes</span>
        </div>
        <nav className="sidebar-nav">
          <NavLink to="/projects" className="nav-link" onClick={closeMenu} end>
            Projetos
          </NavLink>
          {isAdmin && (
            <NavLink to="/users" className="nav-link" onClick={closeMenu}>
              Usuários
            </NavLink>
          )}
          {id && (
            <>
              <div className="nav-section">Projeto atual</div>
              <NavLink to={`/projects/${id}/patents`} className="nav-link" onClick={closeMenu}>
                Patentes
              </NavLink>
              <NavLink to={`/projects/${id}/import`} className="nav-link" onClick={closeMenu}>
                Importação
              </NavLink>
              <NavLink
                to={`/projects/${id}/harmonization`}
                className="nav-link"
                onClick={closeMenu}
              >
                Harmonização
              </NavLink>
              <NavLink to={`/projects/${id}/reports`} className="nav-link" onClick={closeMenu}>
                Relatórios
              </NavLink>
            </>
          )}
        </nav>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <button
            type="button"
            className="menu-toggle"
            aria-label="Abrir menu"
            onClick={() => setMenuOpen((v) => !v)}
          >
            Menu
          </button>
          <div />
          <div className="topbar-user">
            <span>
              {displayName}
              {user?.userLevel ? ` · ${user.userLevel}` : ''}
            </span>
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => void logout()}>
              Sair
            </button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
      {menuOpen && (
        <div
          className="modal-backdrop"
          style={{ zIndex: 30 }}
          onClick={closeMenu}
          aria-hidden="true"
        />
      )}
    </div>
  );
}
