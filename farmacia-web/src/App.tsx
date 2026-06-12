import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/lib/queryClient'
import { AppShell } from '@/components/layout/AppShell'
import { RequireAuth } from '@/components/auth/RequireAuth'
import { LoginPage } from '@/pages/LoginPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { MedicamentosPage } from '@/pages/MedicamentosPage'
import { VendasPage } from '@/pages/VendasPage'
import { EstoquePage } from '@/pages/EstoquePage'
import { ReceitasPage } from '@/pages/ReceitasPage'
import { CadastrosPage } from '@/pages/CadastrosPage'
import { ComprasPage } from '@/pages/ComprasPage'

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <RequireAuth>
                <AppShell />
              </RequireAuth>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="medicamentos" element={<MedicamentosPage />} />
            <Route path="vendas" element={<VendasPage />} />
            <Route path="estoque" element={<EstoquePage />} />
            <Route path="receitas" element={<ReceitasPage />} />
            <Route path="cadastros" element={<CadastrosPage />} />
            <Route path="compras" element={<ComprasPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
