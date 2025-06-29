import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Eye, EyeOff, AlertCircle } from 'lucide-react'
import { useWebSocket } from '@/contexts/WebSocketContext'

export default function Login({ onLogin }) {
  // Estado para controlar qual aba está ativa (login ou registro)
  const [activeTab, setActiveTab] = useState('login')
  
  // Estado do formulário de login
  const [loginData, setLoginData] = useState({
    email: 'Teste@email.com',
    password: 'Teste@123'
  })
  
  // Estado do formulário de registro
  const [registerData, setRegisterData] = useState({
    username: 'Teste1',
    email: 'Teste1@email.com',
    password: 'Teste@123',
    confirmPassword: 'Teste@123'
  })
  
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  
  // Usar o contexto WebSocket
  const { login, register } = useWebSocket()

  // Lidar com envio do formulário de login
  const handleLoginSubmit = async (e) => {
    e.preventDefault()
    setIsLoading(true)
    setError('')

    // Validação
    if (!loginData.email || !loginData.password) {
      setError('Por favor, preencha todos os campos')
      setIsLoading(false)
      return
    }

    try {
      // Chamar a função de login do WebSocket
      await onLogin({
        email: loginData.email,
        password: loginData.password
      })
    } catch (error) {
      setError(error.message || 'Erro ao fazer login')
      setIsLoading(false)
    }
  }
  
  // Lidar com envio do formulário de registro
  const handleRegisterSubmit = async (e) => {
    e.preventDefault()
    setIsLoading(true)
    setError('')
    
    // Validação
    if (!registerData.username || !registerData.email || 
        !registerData.password || !registerData.confirmPassword) {
      setError('Por favor, preencha todos os campos')
      setIsLoading(false)
      return
    }
    
    if (registerData.password !== registerData.confirmPassword) {
      setError('As senhas não coincidem')
      setIsLoading(false)
      return
    }
    
    try {
      // Chamar a função de registro do WebSocket
      await register({
        username: registerData.username,
        email: registerData.email,
        displayName: registerData.username, // Usar o username como displayName
        password: registerData.password
      })
      
      // Após registro bem-sucedido, mudar para a aba de login
      setActiveTab('login')
      setLoginData(prev => ({
        ...prev,
        email: registerData.email
      }))
      setIsLoading(false)
    } catch (error) {
      setError(error.message || 'Erro ao registrar')
      setIsLoading(false)
    }
  }

  // Atualizar dados do formulário de login
  const handleLoginInputChange = (field, value) => {
    setLoginData(prev => ({ ...prev, [field]: value }))
    if (error) setError('')
  }
  
  // Atualizar dados do formulário de registro
  const handleRegisterInputChange = (field, value) => {
    setRegisterData(prev => ({ ...prev, [field]: value }))
    if (error) setError('')
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-lg p-8">
          <div className="text-center mb-6">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">WhatsUT</h1>
            <p className="text-gray-600">Mensagens distribuídas</p>
          </div>
          
          <Tabs defaultValue={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2 mb-6">
              <TabsTrigger value="login">Login</TabsTrigger>
              <TabsTrigger value="register">Cadastro</TabsTrigger>
            </TabsList>
            
            <TabsContent value="login" className="space-y-6">
              <form onSubmit={handleLoginSubmit} className="space-y-6">
                <div className="space-y-2">
                  <Label htmlFor="login-email">Email ou usuário</Label>
                  <Input
                    id="login-email"
                    type="text"
                    placeholder="Digite seu email ou usuário"
                    value={loginData.email}
                    onChange={(e) => handleLoginInputChange('email', e.target.value)}
                    className="h-12"
                    disabled={isLoading}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="login-password">Senha</Label>
                  <div className="relative">
                    <Input
                      id="login-password"
                      type={showPassword ? 'text' : 'password'}
                      placeholder="Digite sua senha"
                      value={loginData.password}
                      onChange={(e) => handleLoginInputChange('password', e.target.value)}
                      className="h-12 pr-10"
                      disabled={isLoading}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                    >
                      {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                    </button>
                  </div>
                </div>

                {error && activeTab === 'login' && (
                  <div className="flex items-center gap-2 text-red-600 text-sm bg-red-50 p-3 rounded-lg">
                    <AlertCircle size={16} />
                    <span>{error}</span>
                  </div>
                )}

                <Button
                  type="submit"
                  className="w-full h-12 bg-blue-600 hover:bg-blue-700 text-white font-medium"
                  disabled={isLoading}
                >
                  {isLoading ? 'Entrando...' : 'Entrar'}
                </Button>
              </form>
            </TabsContent>
            
            <TabsContent value="register" className="space-y-6">
              <form onSubmit={handleRegisterSubmit} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="register-username">Nome de usuário</Label>
                  <Input
                    id="register-username"
                    type="text"
                    placeholder="Digite seu nome de usuário"
                    value={registerData.username}
                    onChange={(e) => handleRegisterInputChange('username', e.target.value)}
                    className="h-12"
                    disabled={isLoading}
                  />
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="register-email">Email</Label>
                  <Input
                    id="register-email"
                    type="email"
                    placeholder="Digite seu email"
                    value={registerData.email}
                    onChange={(e) => handleRegisterInputChange('email', e.target.value)}
                    className="h-12"
                    disabled={isLoading}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="register-password">Senha</Label>
                  <div className="relative">
                    <Input
                      id="register-password"
                      type={showPassword ? 'text' : 'password'}
                      placeholder="Digite sua senha"
                      value={registerData.password}
                      onChange={(e) => handleRegisterInputChange('password', e.target.value)}
                      className="h-12 pr-10"
                      disabled={isLoading}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                    >
                      {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                    </button>
                  </div>
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="register-confirmPassword">Confirmar senha</Label>
                  <Input
                    id="register-confirmPassword"
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Confirme sua senha"
                    value={registerData.confirmPassword}
                    onChange={(e) => handleRegisterInputChange('confirmPassword', e.target.value)}
                    className="h-12"
                    disabled={isLoading}
                  />
                </div>

                {error && activeTab === 'register' && (
                  <div className="flex items-center gap-2 text-red-600 text-sm bg-red-50 p-3 rounded-lg">
                    <AlertCircle size={16} />
                    <span>{error}</span>
                  </div>
                )}

                <Button
                  type="submit"
                  className="w-full h-12 bg-blue-600 hover:bg-blue-700 text-white font-medium mt-4"
                  disabled={isLoading}
                >
                  {isLoading ? 'Cadastrando...' : 'Cadastrar'}
                </Button>
              </form>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  )
}

