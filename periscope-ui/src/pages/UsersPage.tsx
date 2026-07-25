import { useQuery } from '@tanstack/react-query';
import * as usersApi from '../api/users';
import { ErrorAlert, Loading } from '../components/Feedback';

export function UsersPage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['users'],
    queryFn: usersApi.listUsers,
  });

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Usuários</h1>
          <p>Listagem disponível apenas para administradores.</p>
        </div>
      </div>

      {isError && (
        <ErrorAlert message={error instanceof Error ? error.message : 'Erro ao listar usuários.'} />
      )}

      <div className="card">
        {isLoading ? (
          <Loading />
        ) : !data?.length ? (
          <div className="empty-block">Nenhum usuário encontrado.</div>
        ) : (
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th>Usuário</th>
                  <th>Nome</th>
                  <th>E-mail</th>
                  <th>Nível</th>
                </tr>
              </thead>
              <tbody>
                {data.map((user) => (
                  <tr key={user.id}>
                    <td>{user.username}</td>
                    <td>
                      {[user.firstname, user.lastname].filter(Boolean).join(' ') || '—'}
                    </td>
                    <td>{user.email || '—'}</td>
                    <td>
                      <span className="badge">{user.userLevel}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
