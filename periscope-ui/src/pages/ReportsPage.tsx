import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import * as reportsApi from '../api/reports';
import { ErrorAlert, Loading } from '../components/Feedback';
import type { ReportName } from '../types';

const REPORTS: { value: ReportName; label: string }[] = [
  { value: 'main-applicant', label: 'Principais depositantes' },
  { value: 'main-inventor', label: 'Principais inventores' },
  { value: 'main-ipc', label: 'Principais IPCs' },
  { value: 'application-date', label: 'Data de depósito' },
  { value: 'publication-date', label: 'Data de publicação' },
];

export function ReportsPage() {
  const { id: projectId = '' } = useParams();
  const [report, setReport] = useState<ReportName>('main-applicant');

  const reportQuery = useQuery({
    queryKey: ['report', projectId, report],
    queryFn: () => reportsApi.getReport(projectId, report, 15),
    enabled: !!projectId,
  });

  const chartData = useMemo(() => {
    const items = reportQuery.data?.items || [];
    return items.map((item) => ({
      name: String(item.key),
      value: Number(item.value),
    }));
  }, [reportQuery.data]);

  const useLineLike = report === 'application-date' || report === 'publication-date';

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Relatórios</h1>
          <p>Visualize indicadores do projeto em gráfico e tabela.</p>
        </div>
        <Link className="btn btn-secondary" to={`/projects/${projectId}/patents`}>
          Patentes
        </Link>
      </div>

      <div className="card" style={{ marginBottom: '1rem' }}>
        <div className="field">
          <label htmlFor="report">Relatório</label>
          <select
            id="report"
            data-testid="report-select"
            value={report}
            onChange={(e) => setReport(e.target.value as ReportName)}
          >
            {REPORTS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {reportQuery.isLoading && <Loading />}
      {reportQuery.isError && (
        <ErrorAlert
          message={
            reportQuery.error instanceof Error
              ? reportQuery.error.message
              : 'Erro ao carregar relatório.'
          }
        />
      )}

      {reportQuery.data && (
        <>
          <div className="card" data-testid="report-chart">
            <h2>{reportQuery.data.label || REPORTS.find((r) => r.value === report)?.label}</h2>
            {chartData.length === 0 ? (
              <div className="empty-block">Sem dados para este relatório.</div>
            ) : (
              <div style={{ width: '100%', height: 360 }}>
                <ResponsiveContainer>
                  <BarChart
                    data={chartData}
                    margin={{ top: 8, right: 16, left: 8, bottom: useLineLike ? 48 : 64 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#d5dde6" />
                    <XAxis
                      dataKey="name"
                      interval={0}
                      angle={-35}
                      textAnchor="end"
                      height={80}
                      tick={{ fontSize: 11 }}
                    />
                    <YAxis allowDecimals={false} />
                    <Tooltip />
                    <Bar dataKey="value" fill="#1a6f9a" name="Quantidade" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>

          <div className="card" data-testid="report-table">
            <h2>Tabela</h2>
            {chartData.length === 0 ? (
              <div className="empty-block">Sem itens.</div>
            ) : (
              <div className="table-wrap">
                <table className="data">
                  <thead>
                    <tr>
                      <th>Chave</th>
                      <th>Valor</th>
                    </tr>
                  </thead>
                  <tbody>
                    {chartData.map((row) => (
                      <tr key={row.name}>
                        <td>{row.name}</td>
                        <td>{row.value}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
