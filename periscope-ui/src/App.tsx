import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { AdminRoute, ProtectedRoute } from './components/ProtectedRoute';
import { AuthProvider } from './hooks/AuthProvider';
import { HarmonizationPage } from './pages/HarmonizationPage';
import { ImportPage } from './pages/ImportPage';
import { LoginPage } from './pages/LoginPage';
import { PatentDetailPage } from './pages/PatentDetailPage';
import { PatentsPage } from './pages/PatentsPage';
import { ProjectsPage } from './pages/ProjectsPage';
import { ReportsPage } from './pages/ReportsPage';
import { UsersPage } from './pages/UsersPage';

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<Navigate to="/projects" replace />} />
            <Route path="/projects" element={<ProjectsPage />} />
            <Route path="/projects/:id/patents" element={<PatentsPage />} />
            <Route path="/projects/:id/patents/:patentId" element={<PatentDetailPage />} />
            <Route path="/projects/:id/import" element={<ImportPage />} />
            <Route path="/projects/:id/harmonization" element={<HarmonizationPage />} />
            <Route path="/projects/:id/reports" element={<ReportsPage />} />
            <Route element={<AdminRoute />}>
              <Route path="/users" element={<UsersPage />} />
            </Route>
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/projects" replace />} />
      </Routes>
    </AuthProvider>
  );
}
