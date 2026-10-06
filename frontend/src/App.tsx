import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  ConfigProvider,
  DatePicker,
  Form,
  Input,
  Layout,
  Menu,
  Modal,
  Radio,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import viVN from 'antd/locale/vi_VN'
import {
  CalendarOutlined,
  DashboardOutlined,
  ClockCircleOutlined,
  LeftOutlined,
  LockOutlined,
  LogoutOutlined,
  RightOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  BrowserRouter,
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom'
import './App.css'

const { Content, Header } = Layout
const { Title, Text } = Typography

const API_BASE = 'http://localhost:8080/api/v1'
const TOKEN_KEY = 'lms-access-token'

function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`
  return fetch(`${API_BASE}${path}`, { ...options, headers })
}

type UserRole = 'EMPLOYEE' | 'MANAGER' | 'HR'

type SessionUser = {
  id: number
  name: string
  role: UserRole
  email: string
  department: string
}

type AuthContextValue = {
  user: SessionUser | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

// API role mapping
function apiRoleToUiRole(apiRole: string): UserRole {
  if (apiRole === 'ROLE_HR_ADMIN') return 'HR'
  if (apiRole === 'ROLE_MANAGER') return 'MANAGER'
  return 'EMPLOYEE'
}

const AUTH_STORAGE_KEY = 'lms-demo-user'
const PUBLIC_HOLIDAYS = [
  { date: '2026-09-02', name: 'Quốc khánh' },
  { date: '2027-01-01', name: 'Tết Dương lịch' },
  { date: '2027-02-07', name: 'Tết Nguyên đán', note: 'Lịch nghỉ nhiều ngày sẽ theo thông báo chính thức.' },
  { date: '2027-04-16', name: 'Giỗ Tổ Hùng Vương' },
  { date: '2027-04-30', name: 'Ngày Giải phóng miền Nam' },
  { date: '2027-05-01', name: 'Ngày Quốc tế Lao động' },
  { date: '2027-09-02', name: 'Quốc khánh' },
]
const PUBLIC_HOLIDAY_DATES = new Set(PUBLIC_HOLIDAYS.map((holiday) => holiday.date))

function isWeekend(date: Date) {
  const day = date.getDay()
  return day === 0 || day === 6
}

function formatIsoDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function calculateEstimatedLeaveDays(startValue: Date | null, endValue: Date | null, shift: 'FULL' | 'AM' | 'PM' = 'FULL') {
  if (!startValue || !endValue) {
    return 0
  }

  const start = new Date(startValue)
  const end = new Date(endValue)

  if (end < start) {
    return 0
  }

  let total = 0
  const cursor = new Date(start)

  while (cursor <= end) {
    const isoDate = formatIsoDate(cursor)
    const isHoliday = PUBLIC_HOLIDAY_DATES.has(isoDate)

    if (!isWeekend(cursor) && !isHoliday) {
      total += 1
    }

    cursor.setDate(cursor.getDate() + 1)
  }

  if (shift === 'AM' || shift === 'PM') {
    return Number((total * 0.5).toFixed(1))
  }

  return Number(total.toFixed(1))
}

const authContext = {
  user: null,
  isAuthenticated: false,
  login: async () => undefined,
  logout: () => undefined,
} as AuthContextValue

const AuthContext = createContext<AuthContextValue>(authContext)

function getStoredUser(): SessionUser | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as SessionUser
    return parsed && parsed.email ? parsed : null
  } catch {
    return null
  }
}

function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(() => getStoredUser())

  useEffect(() => {
    if (user) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      localStorage.removeItem(TOKEN_KEY)
    }
  }, [user])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    isAuthenticated: Boolean(user),
    login: async (email: string, password: string) => {
      const normalizedEmail = email.trim().toLowerCase()
      const normalizedPassword = password.trim()

      if (!normalizedEmail || !normalizedPassword) {
        throw new Error('Email và mật khẩu không được để trống.')
      }

      // Call real backend login API
      const loginRes = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: normalizedEmail, password: normalizedPassword }),
      })

      if (!loginRes.ok) {
        if (loginRes.status === 401 || loginRes.status === 403) {
          throw new Error('Email hoặc mật khẩu chưa chính xác. Mật khẩu dùng thử: password')
        }
        throw new Error('Đăng nhập không thành công. Vui lòng thử lại.')
      }

      const loginData = await loginRes.json()
      const token: string = loginData.accessToken
      localStorage.setItem(TOKEN_KEY, token)

      // Fetch real profile from backend
      const profileRes = await fetch(`${API_BASE}/users/me`, {
        headers: { Authorization: `Bearer ${token}` },
      })

      if (!profileRes.ok) {
        localStorage.removeItem(TOKEN_KEY)
        throw new Error('Không thể tải thông tin tài khoản. Vui lòng thử lại.')
      }

      const profile = await profileRes.json()
      const sessionUser: SessionUser = {
        id: profile.id,
        name: profile.fullName,
        role: apiRoleToUiRole(profile.role),
        email: profile.email,
        department: profile.department?.name ?? 'Không rõ',
      }
      setUser(sessionUser)
    },
    logout: () => setUser(null),
  }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

function useAuth() {
  return useContext(AuthContext)
}

function getDefaultPageForRole(role: UserRole): string {
  switch (role) {
    case 'HR': return '/dashboard'
    case 'MANAGER': return '/dashboard'
    case 'EMPLOYEE': return '/dashboard'
    default: return '/dashboard'
  }
}

const ROUTE_ROLE_MAP: Record<string, UserRole[] | undefined> = {
  '/dashboard': undefined,
  '/calendar': undefined,
  '/approvals': ['MANAGER', 'HR'],
  '/people': ['HR'],
  '/payroll': ['HR'],
}

function isPathAllowedForRole(path: string, role: UserRole): boolean {
  const allowedRoles = ROUTE_ROLE_MAP[path]
  if (!allowedRoles) return true
  return allowedRoles.includes(role)
}

function ProtectedRoute({ allowedRoles }: { allowedRoles?: UserRole[] }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    const defaultPage = getDefaultPageForRole(user.role)
    return <Navigate to={defaultPage} replace />
  }

  return <Outlet />
}

function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login, isAuthenticated, user } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()

  const rawFrom = (location.state as { from?: string } | null)?.from

  useEffect(() => {
    if (isAuthenticated && user) {
      const defaultPage = getDefaultPageForRole(user.role)
      const target = rawFrom && isPathAllowedForRole(rawFrom, user.role) ? rawFrom : defaultPage
      navigate(target, { replace: true })
    }
  }, [isAuthenticated, user, rawFrom, navigate])

  const handleQuickFill = (email: string) => {
    form.setFieldsValue({
      email,
      password: 'password',
    })
    setError(null)
  }

  const handleSubmit = async (values: { email: string; password: string }) => {
    setLoading(true)
    setError(null)

    try {
      await login(values.email.trim(), values.password.trim())
    } catch (authError) {
      setError(authError instanceof Error ? authError.message : 'Đăng nhập không thành công.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <div className="login-layout">
        <aside className="login-aside">
          <Link to="/login" className="brand-lockup" aria-label="NghỉPhép+, trang đăng nhập">
            <img src="/brand-mark.svg" alt="" />
            <span className="brand-copy">
              <strong>NghỉPhép<span>+</span></strong>
              <small>Quản lý nghỉ phép</small>
            </span>
          </Link>

          <div className="login-welcome">
            <p className="login-eyebrow">KHÔNG GIAN LÀM VIỆC AN TÂM</p>
            <h1>Mỗi ngày nghỉ,<br />một kế hoạch rõ ràng.</h1>
            <p>Theo dõi ngày phép, lịch nhóm và các yêu cầu trên cùng một nơi.</p>
          </div>

          <div className="login-calendar" aria-label="Lịch minh họa tháng 10 năm 2026">
            <div className="login-calendar-heading">
              <span>Lịch nhóm</span>
              <strong>Tháng 10, 2026</strong>
            </div>
            <div className="login-calendar-grid" aria-hidden="true">
              {['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].map((day) => <span key={day} className="weekday">{day}</span>)}
              {Array.from({ length: 35 }, (_, index) => {
                const date = index - 2
                const day = ((date + 30) % 31) + 1
                const outside = date < 1 || date > 31
                return <span key={index} className={`${outside ? 'outside' : ''} ${[8, 14, 23].includes(day) && !outside ? 'away' : ''}`}>{day}</span>
              })}
            </div>
            <div className="calendar-legend"><i /> Có thành viên nghỉ phép</div>
          </div>

          <div className="login-demo-list">
            <span>TÀI KHOẢN TRẢI NGHIỆM (BẤM ĐỂ CHỌN NHANH)</span>
            <div
              className="demo-account-item"
              onClick={() => handleQuickFill('nhanvien@lms.local')}
              title="Bấm để tự điền thông tin"
            >
              <b>Nhân viên (VN)</b>
              <code>nhanvien@lms.local</code>
            </div>
            <div
              className="demo-account-item"
              onClick={() => handleQuickFill('sarah.engineer@lms.local')}
              title="Bấm để tự điền thông tin"
            >
              <b>Nhân viên (Global)</b>
              <code>sarah.engineer@lms.local</code>
            </div>
            <div
              className="demo-account-item"
              onClick={() => handleQuickFill('quanly@lms.local')}
              title="Bấm để tự điền thông tin"
            >
              <b>Quản lý (VN)</b>
              <code>quanly@lms.local</code>
            </div>
            <div
              className="demo-account-item"
              onClick={() => handleQuickFill('david.manager@lms.local')}
              title="Bấm để tự điền thông tin"
            >
              <b>Quản lý (Global)</b>
              <code>david.manager@lms.local</code>
            </div>
            <div
              className="demo-account-item"
              onClick={() => handleQuickFill('hr@lms.local')}
              title="Bấm để tự điền thông tin"
            >
              <b>Nhân sự (HR)</b>
              <code>hr@lms.local</code>
            </div>
            <div
              className="demo-account-item"
              onClick={() => handleQuickFill('admin@lms.local')}
              title="Bấm để tự điền thông tin"
            >
              <b>Quản trị (Admin)</b>
              <code>admin@lms.local</code>
            </div>
            <small>Mật khẩu dùng thử: <code>password</code></small>
          </div>
        </aside>

        <Card className="login-card" variant="borderless">
          <div className="login-card-heading">
            <span className="login-card-icon"><UserOutlined /></span>
            <Title level={2}>Chào mừng trở lại</Title>
            <Text type="secondary">Đăng nhập để tiếp tục quản lý ngày phép của bạn.</Text>
          </div>

          {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}

          <Form form={form} layout="vertical" onFinish={handleSubmit} size="large" requiredMark={false} colon={false}>
            <Form.Item
              name="email"
              label="Email làm việc"
              normalize={(value: string) => value.trim()}
              rules={[
                { required: true, whitespace: true, message: 'Vui lòng nhập email.' },
                { type: 'email', message: 'Email chưa đúng định dạng.' },
                { pattern: /^\S+$/, message: 'Email không được chứa khoảng trắng.' },
              ]}
            >
              <Input prefix={<UserOutlined />} placeholder="ten@congty.vn" autoComplete="username" />
            </Form.Item>

            <Form.Item
              name="password"
              label="Mật khẩu"
              normalize={(value: string) => value.trim()}
              rules={[
                { required: true, whitespace: true, message: 'Vui lòng nhập mật khẩu.' },
                { pattern: /^\S+$/, message: 'Mật khẩu không được chứa khoảng trắng.' },
              ]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="Nhập mật khẩu" autoComplete="current-password" />
            </Form.Item>

            <Button type="primary" htmlType="submit" block loading={loading} className="login-submit">
              Đăng nhập <span aria-hidden="true">→</span>
            </Button>
          </Form>
          <p className="login-security-note"><LockOutlined /> Thông tin đăng nhập được bảo vệ an toàn</p>
        </Card>
      </div>
    </main>
  )
}

const USER_NAMES: Record<number, string> = {
  1: 'System Admin',
  2: 'David Manager',
  3: 'Sarah Engineer',
  4: 'Nguyễn Thảo My',
  5: 'Trần Minh Quân',
  6: 'Phạm Hoài Nhi',
}

const LEAVE_TYPE_NAME_MAP: Record<number, string> = {
  1: 'Nghỉ phép năm',
  2: 'Nghỉ ốm',
  3: 'Nghỉ không lương',
  4: 'Nghỉ thai sản',
}

function DashboardPage() {
  const { user } = useAuth()
  const [isLeaveModalOpen, setIsLeaveModalOpen] = useState(false)
  const [balances, setBalances] = useState<any[]>([])
  const [recentRequests, setRecentRequests] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const today = new Date()
  const todayIso = formatIsoDate(today)

  const loadData = async () => {
    setLoading(true)
    try {
      const [balRes, leavesRes] = await Promise.all([
        apiFetch('/leaves/balances'),
        apiFetch('/leaves/my'),
      ])
      if (balRes.ok) {
        const balData = await balRes.json()
        setBalances(balData)
      }
      if (leavesRes.ok) {
        const leavesData = await leavesRes.json()
        setRecentRequests(leavesData)
      }
    } catch (err) {
      console.error('Error loading dashboard data:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const annualBal = balances.find((b) => b.leaveTypeId === 1)
  const annualRemaining = annualBal ? (annualBal.accruedDays + annualBal.carriedOverDays - annualBal.usedDays) : 12
  const annualUsed = annualBal?.usedDays ?? 0
  const pendingCount = recentRequests.filter((r) => r.status === 'SUBMITTED' || r.status === 'ESCALATED').length
  const sickBal = balances.find((b) => b.leaveTypeId === 2)
  const sickUsed = sickBal?.usedDays ?? 0

  const summaryCards = [
    { title: 'Phép năm còn lại', value: String(annualRemaining).replace('.', ','), unit: 'ngày', foot: 'Trong năm 2026', tone: '#6366f1', progress: Math.min(100, Math.round((annualRemaining / 15) * 100)) },
    { title: 'Đã sử dụng', value: String(annualUsed).padStart(2, '0'), unit: 'ngày', foot: 'Phép năm', tone: '#10b981', progress: Math.min(100, Math.round((annualUsed / 15) * 100)) },
    { title: 'Đang chờ duyệt', value: String(pendingCount).padStart(2, '0'), unit: 'đơn', foot: 'Cần theo dõi', tone: '#d97706', progress: pendingCount > 0 ? 50 : 0 },
    { title: 'Nghỉ ốm đã dùng', value: String(sickUsed).replace('.', ','), unit: 'ngày', foot: 'Từ đầu năm', tone: '#e06b62', progress: Math.min(100, Math.round((sickUsed / 10) * 100)) },
  ]

  const holidays = PUBLIC_HOLIDAYS.filter((holiday) => holiday.date >= todayIso).slice(0, 4)

  const STATUS_NAME_MAP: Record<string, string> = {
    SUBMITTED: 'Đang chờ',
    APPROVED: 'Đã duyệt',
    REJECTED: 'Từ chối',
    ESCALATED: 'Đã tăng cấp',
    CANCELLED: 'Đã hủy',
  }

  const STATUS_COLOR_MAP: Record<string, string> = {
    'Đã duyệt': 'green',
    'Đã tăng cấp': 'orange',
    'Đang chờ': 'blue',
    'Từ chối': 'red',
  }

  const tableData = recentRequests.map((r) => ({
    key: String(r.id),
    type: LEAVE_TYPE_NAME_MAP[r.leaveTypeId] || 'Nghỉ phép',
    dates: `${r.startDate} - ${r.endDate}`,
    amount: `${r.totalBillableDays}d`,
    status: STATUS_NAME_MAP[r.status] || r.status,
  }))

  const columns = [
    { title: 'Loại', dataIndex: 'type', key: 'type' },
    { title: 'Thời gian', dataIndex: 'dates', key: 'dates' },
    { title: 'Số ngày', dataIndex: 'amount', key: 'amount' },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) => <Tag color={STATUS_COLOR_MAP[value] || 'default'}>{value}</Tag>,
    },
  ]

  const handleExport = async () => {
    try {
      const res = await apiFetch('/reports/payroll?month=2026-09&format=csv')
      if (res.ok) {
        const blob = await res.blob()
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = 'bao_cao_nghi_phep.csv'
        document.body.appendChild(a)
        a.click()
        a.remove()
        window.URL.revokeObjectURL(url)
        message.success('Đã tải xuống báo cáo nghỉ phép!')
      } else {
        message.info('Báo cáo tổng hợp dành cho Quản lý & Nhân sự.')
      }
    } catch {
      message.error('Lỗi khi tải báo cáo.')
    }
  }

  return (
    <>
      <Space orientation="vertical" size="large" style={{ width: '100%' }}>
        <div className="dashboard-header">
          <div>
            <Text className="dashboard-date">{new Intl.DateTimeFormat('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }).format(today)}</Text>
            <Title level={3} style={{ marginBottom: 4 }}>Chào mừng trở lại, {user?.name}</Title>
            <Text type="secondary">{user?.role === 'EMPLOYEE' ? 'Nhân viên' : user?.role === 'MANAGER' ? 'Quản lý' : 'Nhân sự'} · {user?.department}</Text>
          </div>
          <div className="header-actions">
            <Button type="primary" onClick={() => setIsLeaveModalOpen(true)}>Đăng ký nghỉ</Button>
            <Button onClick={handleExport}>Xuất báo cáo</Button>
          </div>
        </div>

        <Row gutter={[16, 16]}>
          {summaryCards.map((card) => (
            <Col xs={24} sm={12} xl={6} key={card.title}>
              <Card className="metric-card" variant="borderless">
                <div className="metric-heading"><Badge color={card.tone} text={card.title} /><span className="metric-icon"><CalendarOutlined /></span></div>
                <Title level={2} className="metric-value">{card.value}<small>{card.unit}</small></Title>
                <div className="metric-progress"><span style={{ width: `${card.progress}%`, background: card.tone }} /></div>
                <Text type="secondary">{card.foot}</Text>
              </Card>
            </Col>
          ))}
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={16}>
            <Card className="panel-card" title="Yêu cầu nghỉ gần đây" variant="borderless">
              <Table
                loading={loading}
                columns={columns}
                dataSource={tableData}
                pagination={false}
                size="small"
                scroll={{ x: 620 }}
                className="dashboard-table"
                locale={{ emptyText: 'Chưa có yêu cầu nghỉ nào.' }}
              />
            </Card>
          </Col>

          <Col xs={24} lg={8}>
            <Card className="panel-card" title="Ngày nghỉ sắp tới" variant="borderless">
              <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                {holidays.map((holiday) => (
                  <div key={holiday.date} className="holiday-item">
                    <div>
                      <Text strong>{holiday.name}</Text>
                      <div><Text type="secondary">{new Intl.DateTimeFormat('vi-VN', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(`${holiday.date}T12:00:00`))}</Text></div>
                    </div>
                    <Tag color="purple">Nghỉ lễ</Tag>
                  </div>
                ))}
                <Text className="holiday-note">Lịch nghỉ Tết có thể được điều chỉnh theo thông báo chính thức.</Text>
              </Space>
            </Card>
          </Col>
        </Row>
      </Space>

      <LeaveRequestModal open={isLeaveModalOpen} onCancel={() => setIsLeaveModalOpen(false)} onSuccess={loadData} />
    </>
  )
}

function LeaveRequestModal({ open, onCancel, onSuccess }: { open: boolean; onCancel: () => void; onSuccess?: () => void }) {
  const [leaveType, setLeaveType] = useState('1')
  const [shift, setShift] = useState<'FULL' | 'AM' | 'PM'>('FULL')
  const [range, setRange] = useState<[Date | null, Date | null]>([null, null])
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [availableBalance, setAvailableBalance] = useState(12)

  const isUnpaid = leaveType === '3'

  useEffect(() => {
    if (open) {
      if (leaveType === '3') {
        setAvailableBalance(999)
        return
      }
      apiFetch('/leaves/balances')
        .then((res) => res.json())
        .then((data) => {
          if (Array.isArray(data)) {
            const bal = data.find((b: any) => b.leaveTypeId === Number(leaveType))
            if (bal) {
              const remaining = (bal.accruedDays || 0) + (bal.carriedOverDays || 0) - (bal.usedDays || 0)
              setAvailableBalance(Math.max(0, remaining))
            }
          }
        })
        .catch(console.error)
    }
  }, [open, leaveType])

  const estimatedDays = useMemo(() => {
    const [start, end] = range
    return calculateEstimatedLeaveDays(start, end, shift)
  }, [range, shift])

  const isOverBalance = !isUnpaid && estimatedDays > availableBalance
  const isMissingDates = !range[0] || !range[1]
  const isMissingReason = !reason.trim()
  const hasConflict = !isUnpaid && estimatedDays > availableBalance * 0.7 && !isOverBalance
  const isDisabled = isMissingDates || isMissingReason || isOverBalance || submitting

  const handleSubmit = async () => {
    if (!range[0] || !range[1]) return
    setSubmitting(true)
    try {
      const startHalf = shift === 'PM' ? 'AFTERNOON' : 'MORNING'
      const endHalf = shift === 'AM' ? 'MORNING' : 'AFTERNOON'
      const res = await apiFetch('/leaves/submit', {
        method: 'POST',
        body: JSON.stringify({
          leaveTypeId: Number(leaveType),
          startDate: formatIsoDate(range[0]),
          endDate: formatIsoDate(range[1]),
          startHalf,
          endHalf,
          reason: reason.trim(),
          totalBillableDays: estimatedDays,
        }),
      })

      if (!res.ok) {
        const errorData = await res.json().catch(() => null)
        throw new Error(errorData?.message || 'Không thể gửi đơn nghỉ phép.')
      }

      message.success('Gửi đơn nghỉ phép thành công!')
      setReason('')
      setRange([null, null])
      onSuccess?.()
      onCancel()
    } catch (err: any) {
      message.error(err.message || 'Lỗi khi gửi đơn.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title="Đăng ký nghỉ phép"
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>Hủy</Button>,
        <Button key="submit" type="primary" loading={submitting} disabled={isDisabled} onClick={handleSubmit}>
          Gửi yêu cầu
        </Button>,
      ]}
      width={620}
    >
      <Form layout="vertical">
        <Form.Item label="Loại nghỉ phép">
          <Select
            value={leaveType}
            onChange={setLeaveType}
            options={[
              { value: '1', label: 'Nghỉ phép năm' },
              { value: '2', label: 'Nghỉ ốm' },
              { value: '3', label: 'Nghỉ không lương (không trừ quỹ phép)' },
              { value: '4', label: 'Nghỉ thai sản' },
            ]}
          />
        </Form.Item>

        <Form.Item label="Khoảng thời gian" required>
          <DatePicker.RangePicker
            style={{ width: '100%' }}
            onChange={(value) => {
              if (!value) {
                setRange([null, null])
                return
              }

              const [start, end] = value
              setRange([start ? start.toDate() : null, end ? end.toDate() : null])
            }}
          />
        </Form.Item>

        <Form.Item label="Cách tính buổi nghỉ">
          <Radio.Group value={shift} onChange={(event) => setShift(event.target.value)}>
            <Radio value="FULL">Cả ngày</Radio>
            <Radio value="AM">Sáng</Radio>
            <Radio value="PM">Chiều</Radio>
          </Radio.Group>
        </Form.Item>

        <Form.Item
          label="Lý do"
          required
          help={isMissingReason && !isMissingDates ? 'Bắt buộc nhập lý do xin nghỉ' : undefined}
          validateStatus={isMissingReason && !isMissingDates ? 'error' : undefined}
        >
          <Input.TextArea
            rows={3}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Mô tả ngắn gọn lý do nghỉ phép"
          />
        </Form.Item>

        <div className="leave-summary">
          <div>
            <Text type="secondary">Số ngày làm việc ước tính</Text>
            <div className="summary-value">{estimatedDays || 0}d</div>
          </div>
          <div>
            <Text type="secondary">Số ngày còn lại</Text>
            <div className="summary-value">{isUnpaid ? 'Không giới hạn' : `${availableBalance}d`}</div>
          </div>
        </div>

        {isOverBalance && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            title="Vượt quá số ngày phép khả dụng"
            description={`Bạn xin nghỉ ${estimatedDays} ngày nhưng chỉ còn ${availableBalance} ngày phép năm. Để gửi yêu cầu, vui lòng chọn số ngày nhỏ hơn hoặc đổi loại nghỉ sang "Nghỉ không lương".`}
          />
        )}

        {isMissingReason && !isMissingDates && (
          <Alert
            type="info"
            showIcon
            style={{ marginTop: 12 }}
            description="Vui lòng điền lý do xin nghỉ để kích hoạt nút 'Gửi yêu cầu'."
          />
        )}

        {hasConflict && (
          <Alert
            type="warning"
            showIcon
            style={{ marginTop: 12 }}
            title="Có nguy cơ xung đột lịch làm việc"
            description="Yêu cầu này trùng với nhiều ngày nghỉ của đội. Vui lòng xác nhận với quản lý trước khi gửi."
          />
        )}
      </Form>
    </Modal>
  )
}

function ApprovalPage() {
  const [approvals, setApprovals] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState<number | null>(null)

  const loadApprovals = async () => {
    setLoading(true)
    try {
      const res = await apiFetch('/approvals')
      if (res.ok) {
        const data = await res.json()
        setApprovals(data)
      }
    } catch (err) {
      console.error('Error fetching approvals:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadApprovals()
  }, [])

  const handleDecision = async (id: number, decision: 'APPROVE' | 'REJECT') => {
    setActionLoading(id)
    try {
      const res = await apiFetch(`/approvals/${id}/act`, {
        method: 'POST',
        body: JSON.stringify({
          decision,
          decisionReason: decision === 'APPROVE' ? 'Đã phê duyệt qua hệ thống' : 'Từ chối yêu cầu nghỉ phép',
        }),
      })
      if (!res.ok) {
        const err = await res.json().catch(() => null)
        throw new Error(err?.message || 'Thao tác không thành công.')
      }
      message.success(decision === 'APPROVE' ? 'Đã phê duyệt yêu cầu!' : 'Đã từ chối yêu cầu.')
      await loadApprovals()
    } catch (err: any) {
      message.error(err.message || 'Lỗi xử lý')
    } finally {
      setActionLoading(null)
    }
  }

  const summaryCards = [
    { title: 'Yêu cầu chờ duyệt', value: String(approvals.length).padStart(2, '0'), tone: 'blue' },
    { title: 'Khẩn cấp hôm nay', value: String(approvals.filter((a) => a.backdated).length).padStart(2, '0'), tone: 'gold' },
    { title: 'Tăng cấp', value: String(approvals.filter((a) => a.status === 'ESCALATED').length).padStart(2, '0'), tone: 'red' },
  ]

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <div className="dashboard-header">
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>Phê duyệt nghỉ phép</Title>
          <Text type="secondary">Danh sách ưu tiên cho nhân sự trực thuộc ({approvals.length} yêu cầu cần xử lý)</Text>
        </div>
        <Button type="primary" onClick={loadApprovals} loading={loading}>Làm mới</Button>
      </div>

      <Row gutter={[16, 16]}>
        {summaryCards.map((card) => (
          <Col xs={24} md={8} key={card.title}>
            <Card className="metric-card" variant="borderless">
              <Badge color={card.tone} text={card.title} />
              <Title level={2} style={{ marginTop: 12, marginBottom: 4 }}>{card.value}</Title>
            </Card>
          </Col>
        ))}
      </Row>

      {approvals.length === 0 && !loading && (
        <Card variant="borderless" style={{ textAlign: 'center', padding: '40px 0' }}>
          <Text type="secondary">Hiện không có yêu cầu nào đang chờ phê duyệt.</Text>
        </Card>
      )}

      <Row gutter={[16, 16]}>
        {approvals.map((request) => {
          const empName = USER_NAMES[request.userId] || `Nhân sự #${request.userId}`
          const typeName = LEAVE_TYPE_NAME_MAP[request.leaveTypeId] || 'Nghỉ phép'
          const isUrgent = request.backdated || request.status === 'ESCALATED'

          return (
            <Col xs={24} lg={12} key={request.id}>
              <Card className="approval-card" variant="borderless">
                <div className="approval-card-top">
                  <div>
                    <Text type="secondary">REQ-#{request.id}</Text>
                    <Title level={4} style={{ margin: '4px 0 0' }}>{empName}</Title>
                  </div>
                  <div className={`sla-pill ${isUrgent ? 'warning' : 'good'}`}>
                    {request.status === 'ESCALATED' ? 'Tăng cấp' : request.backdated ? 'Khẩn cấp' : 'Bình thường'}
                  </div>
                </div>

                <div className="approval-meta-grid">
                  <div>
                    <Text type="secondary">Loại nghỉ</Text>
                    <div>{typeName}</div>
                  </div>
                  <div>
                    <Text type="secondary">Thời lượng</Text>
                    <div>{request.totalBillableDays} ngày</div>
                  </div>
                  <div>
                    <Text type="secondary">Khoảng thời gian</Text>
                    <div>{request.startDate} • {request.endDate}</div>
                  </div>
                  <div>
                    <Text type="secondary">Trạng thái</Text>
                    <div>{request.status}</div>
                  </div>
                </div>

                <Alert
                  type={isUrgent ? 'warning' : 'info'}
                  showIcon
                  title={request.reason ? `Lý do: ${request.reason}` : 'Nghỉ theo kế hoạch'}
                  description={`Gửi lúc: ${request.submittedAt ? request.submittedAt.replace('T', ' ') : 'N/A'}`}
                  className="approval-alert"
                />

                <div className="approval-actions">
                  <Button
                    type="primary"
                    loading={actionLoading === request.id}
                    onClick={() => handleDecision(request.id, 'APPROVE')}
                  >
                    Phê duyệt
                  </Button>
                  <Button
                    danger
                    loading={actionLoading === request.id}
                    onClick={() => handleDecision(request.id, 'REJECT')}
                  >
                    Từ chối
                  </Button>
                </div>
              </Card>
            </Col>
          )
        })}
      </Row>
    </Space>
  )
}

type TeamMember = {
  id: number
  name: string
  initials: string
  role: string
  department: string
  employmentStatus: 'PROBATION' | 'PERMANENT'
  status: 'Hoạt động' | 'Nghỉ việc'
}

type LeaveScheduleEntry = {
  memberId: number
  name: string
  initials: string
  role: string
  department: string
  start: string | null
  end: string | null
  type: 'annual' | 'sick' | 'unpaid' | 'remote' | 'personal'
  status: 'pending' | 'approved'
  reason?: string
}

const INITIAL_MEMBERS: TeamMember[] = [
  { id: 1, name: 'Nguyễn Thảo My', initials: 'TM', role: 'Kỹ sư phần mềm (Frontend)', department: 'Phòng Kỹ thuật', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
  { id: 2, name: 'Trần Quốc Bảo', initials: 'QB', role: 'Kỹ sư phần mềm (Frontend)', department: 'Phòng Kỹ thuật', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
  { id: 3, name: 'Lê Thu Hà', initials: 'TH', role: 'Chuyên viên Thiết kế UI/UX', department: 'Phòng Kỹ thuật', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
  { id: 4, name: 'Phạm Đức Long', initials: 'ĐL', role: 'Kỹ sư phần mềm (Backend)', department: 'Phòng Kỹ thuật', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
  { id: 5, name: 'Nguyễn Hoàng Yến', initials: 'HY', role: 'Kỹ sư Đảm bảo chất lượng (QA)', department: 'Phòng Kỹ thuật', employmentStatus: 'PROBATION', status: 'Hoạt động' },
  { id: 6, name: 'Đỗ Khánh Linh', initials: 'KL', role: 'Chuyên viên Truyền thông', department: 'Phòng Vận hành', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
  { id: 7, name: 'Bùi Thanh Sơn', initials: 'TS', role: 'Kỹ sư Vận hành hệ thống (DevOps)', department: 'Phòng Kỹ thuật', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
  { id: 8, name: 'Trần Minh Quân', initials: 'MQ', role: 'Quản lý Nhân sự & Vận hành', department: 'Phòng Nhân sự', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
  { id: 9, name: 'Phạm Hoài Nhi', initials: 'HN', role: 'Chuyên viên Nhân sự cấp cao (HR Admin)', department: 'Phòng Nhân sự', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
  { id: 10, name: 'Hoàng Mai Anh', initials: 'MA', role: 'Chuyên viên Tuyển dụng & Đãi ngộ', department: 'Phòng Nhân sự', employmentStatus: 'PERMANENT', status: 'Hoạt động' },
]

const INITIAL_SCHEDULE: LeaveScheduleEntry[] = [
  { memberId: 2, name: 'Trần Quốc Bảo', initials: 'QB', role: 'Kỹ sư Frontend', department: 'Phòng Kỹ thuật', start: '2026-10-12', end: '2026-10-14', type: 'annual', status: 'pending', reason: 'Nghỉ việc gia đình' },
  { memberId: 3, name: 'Lê Thu Hà', initials: 'TH', role: 'Chuyên viên UI/UX', department: 'Phòng Kỹ thuật', start: '2026-10-05', end: '2026-10-06', type: 'sick', status: 'pending', reason: 'Nghỉ khám sức khỏe định kỳ' },
  { memberId: 4, name: 'Phạm Đức Long', initials: 'ĐL', role: 'Kỹ sư Backend', department: 'Phòng Kỹ thuật', start: '2026-10-08', end: '2026-10-09', type: 'remote', status: 'pending', reason: 'Làm việc từ xa hỗ trợ bảo trì' },
  { memberId: 5, name: 'Nguyễn Hoàng Yến', initials: 'HY', role: 'Kỹ sư QA', department: 'Phòng Kỹ thuật', start: '2026-10-06', end: '2026-10-06', type: 'remote', status: 'approved', reason: 'Làm việc từ xa' },
  { memberId: 6, name: 'Đỗ Khánh Linh', initials: 'KL', role: 'Chuyên viên Truyền thông', department: 'Phòng Vận hành', start: '2026-10-07', end: '2026-10-09', type: 'annual', status: 'approved', reason: 'Nghỉ phép năm đã được duyệt' },
  { memberId: 7, name: 'Bùi Thanh Sơn', initials: 'TS', role: 'Kỹ sư DevOps', department: 'Phòng Kỹ thuật', start: '2026-10-13', end: '2026-10-13', type: 'sick', status: 'approved', reason: 'Nghỉ ốm phục hồi sau tiểu phẫu' },
  { memberId: 1, name: 'Nguyễn Thảo My', initials: 'TM', role: 'Kỹ sư Frontend', department: 'Phòng Kỹ thuật', start: null, end: null, type: 'annual', status: 'approved' },
  { memberId: 8, name: 'Trần Minh Quân', initials: 'MQ', role: 'Quản lý Nhân sự', department: 'Phòng Nhân sự', start: '2026-10-09', end: '2026-10-09', type: 'annual', status: 'approved', reason: 'Công tác đối ngoại' },
  { memberId: 9, name: 'Phạm Hoài Nhi', initials: 'HN', role: 'HR Admin', department: 'Phòng Nhân sự', start: null, end: null, type: 'annual', status: 'approved' },
  { memberId: 10, name: 'Hoàng Mai Anh', initials: 'MA', role: 'Chuyên viên Nhân sự', department: 'Phòng Nhân sự', start: '2026-10-14', end: '2026-10-15', type: 'annual', status: 'approved', reason: 'Nghỉ phép năm' },
]

function getCurrentMonday(): Date {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const day = today.getDay()
  const offset = (day + 6) % 7 // Monday = 0
  today.setDate(today.getDate() - offset)
  return today
}

function TeamCalendarPage() {
  const { user } = useAuth()
  const [calendarStart, setCalendarStart] = useState<Date>(getCurrentMonday)
  const isHrOrManager = user?.role === 'HR' || user?.role === 'MANAGER'
  const [selectedDept, setSelectedDept] = useState<string>(() => {
    if (user?.role === 'HR') return 'ALL'
    return user?.department || 'ALL'
  })

  // Real data from backend
  const [members, setMembers] = useState<TeamMember[]>([])
  const [schedules, setSchedules] = useState<LeaveScheduleEntry[]>([])
  const [loadingMembers, setLoadingMembers] = useState(false)
  const [loadingSchedule, setLoadingSchedule] = useState(false)

  // Sinh 10 ngày làm việc (Thứ 2 đến Thứ 6 trong 2 tuần liên tiếp)
  const scheduleDays = useMemo(() => {
    const days: Date[] = []
    const cursor = new Date(calendarStart)
    cursor.setHours(0, 0, 0, 0)
    while (days.length < 10) {
      const wd = cursor.getDay()
      if (wd !== 0 && wd !== 6) {
        days.push(new Date(cursor))
      }
      cursor.setDate(cursor.getDate() + 1)
    }
    return days
  }, [calendarStart])

  // Fetch team members once on mount
  useEffect(() => {
    setLoadingMembers(true)
    apiFetch('/leaves/team-members')
      .then((res) => res.ok ? res.json() : [])
      .then((data: any[]) => {
        const mapped: TeamMember[] = data.map((u: any) => {
          const nameParts = u.fullName.trim().split(' ')
          const initials = nameParts.length > 1
            ? `${nameParts[0][0]}${nameParts[nameParts.length - 1][0]}`.toUpperCase()
            : u.fullName.slice(0, 2).toUpperCase()
          return {
            id: u.id,
            name: u.fullName,
            initials,
            role: u.role === 'ROLE_MANAGER' ? 'Quản lý' : u.role === 'ROLE_HR_ADMIN' ? 'Nhân sự' : 'Nhân viên',
            department: u.department || 'Chung',
            employmentStatus: u.employmentStatus === 'PROBATION' ? 'PROBATION' : 'PERMANENT',
            status: u.active ? 'Hoạt động' : 'Nghỉ việc',
          }
        })
        setMembers(mapped)
      })
      .catch(console.error)
      .finally(() => setLoadingMembers(false))
  }, [])

  // Fetch team schedule when date window changes
  useEffect(() => {
    if (scheduleDays.length === 0) return
    const startStr = formatIsoDate(scheduleDays[0])
    const endStr = formatIsoDate(scheduleDays[scheduleDays.length - 1])
    setLoadingSchedule(true)
    apiFetch(`/leaves/team-schedule?start=${startStr}&end=${endStr}`)
      .then((res) => res.ok ? res.json() : [])
      .then((data: any[]) => {
        const mapped: LeaveScheduleEntry[] = data.map((r: any) => {
          // Determine leave type label from leaveTypeId
          const typeMap: Record<number, LeaveScheduleEntry['type']> = {
            1: 'annual',
            2: 'sick',
            3: 'personal',  // unpaid → personal (grey bar)
            4: 'sick',      // maternity → sick (orange bar)
          }
          const statusMap: Record<string, LeaveScheduleEntry['status']> = {
            SUBMITTED: 'pending',
            ESCALATED: 'pending',
            APPROVED: 'approved',
          }
          // Find matching member name
          const memberMatch = members.find((m) => m.id === r.userId)
          const name = memberMatch?.name || `User #${r.userId}`
          const initials = memberMatch?.initials || String(r.userId)
          const role = memberMatch?.role || ''
          const department = memberMatch?.department || ''
          return {
            memberId: r.userId,
            name,
            initials,
            role,
            department,
            start: r.startDate,
            end: r.endDate,
            type: typeMap[r.leaveTypeId] ?? 'annual',
            status: statusMap[r.status] ?? 'pending',
            reason: r.reason,
          }
        })
        setSchedules(mapped)
      })
      .catch(console.error)
      .finally(() => setLoadingSchedule(false))
  }, [scheduleDays, members])

  // Unique departments from members
  const departments = useMemo(() => {
    const depts = new Set(members.map((m) => m.department))
    return Array.from(depts)
  }, [members])

  // Lọc thành viên theo phòng ban lựa chọn
  const filteredMembers = useMemo(() => {
    return members.filter((m) => {
      if (m.status !== 'Hoạt động') return false
      if (selectedDept === 'ALL') return true
      return m.department === selectedDept
    })
  }, [members, selectedDept])

  // Lập danh sách lịch làm việc tương ứng thành viên
  const visibleSchedule = useMemo(() => {
    return filteredMembers.map((member) => {
      const match = schedules.find((s) => s.memberId === member.id)
      if (match) {
        return {
          ...match,
          role: member.role,
          department: member.department,
        }
      }
      return {
        memberId: member.id,
        name: member.name,
        initials: member.initials,
        role: member.role,
        department: member.department,
        start: null,
        end: null,
        type: 'annual' as const,
        status: 'approved' as const,
      }
    })
  }, [filteredMembers, schedules])

  const dateFormatter = useMemo(() => new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }), [])
  const weekdayLabels = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

  const windowLabel = useMemo(() => {
    if (scheduleDays.length === 0) return ''
    const first = scheduleDays[0]
    const last = scheduleDays[scheduleDays.length - 1]
    return `${dateFormatter.format(first)} – ${dateFormatter.format(last)}/${last.getFullYear()}`
  }, [scheduleDays, dateFormatter])

  // Đếm số người vắng mặt mỗi ngày làm việc
  const awayCounts = useMemo(() => {
    return scheduleDays.map((day) => {
      const dateStr = formatIsoDate(day)
      return visibleSchedule.filter((entry) => {
        if (!entry.start || !entry.end) return false
        return entry.start <= dateStr && entry.end >= dateStr
      }).length
    })
  }, [scheduleDays, visibleSchedule])

  const moveCalendar = (days: number) => {
    setCalendarStart((prev) => {
      const next = new Date(prev)
      next.setDate(next.getDate() + days)
      return next
    })
  }

  const showCurrentWeek = () => {
    setCalendarStart(getCurrentMonday())
  }

  const departmentTitle = selectedDept === 'ALL' ? 'Toàn công ty' : selectedDept
  const isLoading = loadingMembers || loadingSchedule

  return (
    <section className="team-calendar-card schedule-panel" aria-labelledby="team-calendar-title">
      <header className="schedule-toolbar">
        <div className="schedule-heading">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <h1 id="team-calendar-title">Lịch nhóm</h1>
            {user?.role === 'HR' && departments.length > 0 && (
              <Select
                size="small"
                value={selectedDept}
                onChange={setSelectedDept}
                style={{ width: 170 }}
                options={[
                  { value: 'ALL', label: 'Toàn đơn vị (HR)' },
                  ...departments.map((d) => ({ value: d, label: d })),
                ]}
              />
            )}
            {isLoading && <Text type="secondary" style={{ fontSize: 12 }}>Đang tải...</Text>}
          </div>
          <p>{departmentTitle} · {windowLabel} · {filteredMembers.length} nhân sự</p>
        </div>
        <div className="schedule-controls">
          <ul className="schedule-legend" aria-label="Chú giải loại nghỉ">
            <li><i className="legend-annual" />Phép năm</li>
            <li><i className="legend-sick" />Nghỉ ốm</li>
            <li><i className="legend-remote" />Làm việc từ xa</li>
            <li><i className="legend-personal" />Nghỉ cá nhân</li>
            <li><i className="legend-pending" />Chờ duyệt</li>
          </ul>
          <div className="schedule-nav" aria-label="Điều hướng lịch">
            <Button aria-label="Hai tuần trước" icon={<LeftOutlined />} onClick={() => moveCalendar(-14)} />
            <Button onClick={showCurrentWeek}>Hôm nay</Button>
            <Button aria-label="Hai tuần sau" icon={<RightOutlined />} onClick={() => moveCalendar(14)} />
          </div>
        </div>
      </header>

      <div className="schedule-scroll">
        <div className="schedule-grid" role="table" aria-label="Lịch nghỉ và hiện diện của nhóm">
          <div className="schedule-row schedule-header-row" role="row">
            <div className="schedule-member-heading" role="columnheader">Thành viên</div>
            {scheduleDays.map((day) => (
              <div className="schedule-day-heading" role="columnheader" key={formatIsoDate(day)}>
                <span>{weekdayLabels[day.getDay()]}</span>
                <strong>{dateFormatter.format(day)}</strong>
              </div>
            ))}
          </div>

          {visibleSchedule.map((member) => {
            const firstIndex = member.start ? scheduleDays.findIndex((day) => formatIsoDate(day) >= member.start!) : -1
            const lastIndex = member.end ? scheduleDays.findLastIndex((day) => formatIsoDate(day) <= member.end!) : -1
            const hasVisibleEntry = firstIndex >= 0 && lastIndex >= firstIndex
            
            // Mask reason based on privacy rules FR-10 / FR-14
            const tooltipReason = isHrOrManager && member.reason ? ` · ${member.reason}` : ''
            const statusLabel = member.status === 'pending' ? 'Chờ duyệt' : 'Đã duyệt'
            const typeLabel =
              member.type === 'annual'
                ? 'Phép năm'
                : member.type === 'sick'
                ? 'Nghỉ ốm'
                : member.type === 'remote'
                ? 'Làm việc từ xa'
                : 'Nghỉ cá nhân'

            return (
              <div className="schedule-row schedule-member-row" role="row" key={`${member.memberId}-${member.name}`}>
                <div className="schedule-member" role="rowheader">
                  <span className="schedule-avatar" aria-hidden="true">{member.initials}</span>
                  <span className="schedule-member-copy">
                    <strong>{member.name}</strong>
                    <small>{member.role}</small>
                  </span>
                </div>
                <div className="schedule-track" role="cell" style={{ gridColumn: '2 / -1' }}>
                  {scheduleDays.map((_, i) => (
                    <div key={i} className="schedule-col-cell" />
                  ))}
                  {hasVisibleEntry ? (
                    <div
                      className={`schedule-bar leave-${member.type} ${member.status}`}
                      style={{
                        gridColumnStart: firstIndex + 1,
                        gridColumnEnd: lastIndex + 2,
                      }}
                      title={`${member.name} · ${statusLabel}${tooltipReason}`}
                    >
                      {member.status === 'pending' && <ClockCircleOutlined />}
                      <span>{typeLabel}</span>
                    </div>
                  ) : (
                    <span className="schedule-present">Có mặt cả kỳ</span>
                  )}
                </div>
              </div>
            )
          })}

          <div className="schedule-row schedule-footer-row" role="row">
            <div className="schedule-member-heading" role="rowheader">Số người vắng</div>
            {awayCounts.map((count, index) => (
              <div className="schedule-away-count" role="cell" key={`${formatIsoDate(scheduleDays[index])}-${index}`}>
                <span className={count >= 3 ? 'busy' : count > 0 ? 'some' : 'none'}>{count}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
      <p className="schedule-privacy-note">
        <LockOutlined /> {isHrOrManager ? 'Tài khoản Quản lý & HR có quyền xem lý do và duyệt nghỉ phép trực tiếp.' : 'Lý do nghỉ riêng tư chỉ hiển thị với người có quyền.'}
      </p>
    </section>
  )
}


function PeoplePage() {
  const [members, setMembers] = useState<TeamMember[]>(() => {
    const saved = localStorage.getItem('lms_company_members')
    if (saved) {
      try { return JSON.parse(saved) } catch { /* ignore */ }
    }
    return INITIAL_MEMBERS
  })

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingMember, setEditingMember] = useState<TeamMember | null>(null)
  const [form] = Form.useForm()

  const saveMembers = (updated: TeamMember[]) => {
    setMembers(updated)
    localStorage.setItem('lms_company_members', JSON.stringify(updated))
  }

  const handleOpenAdd = () => {
    setEditingMember(null)
    form.resetFields()
    form.setFieldsValue({
      department: 'Phòng Kỹ thuật',
      employmentStatus: 'PERMANENT',
      status: 'Hoạt động',
    })
    setIsModalOpen(true)
  }

  const handleOpenEdit = (member: TeamMember) => {
    setEditingMember(member)
    form.setFieldsValue(member)
    setIsModalOpen(true)
  }

  const handleModalSubmit = () => {
    form.validateFields().then((values) => {
      const words = values.name.trim().split(' ')
      const initials = words.length > 1
        ? `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase()
        : values.name.slice(0, 2).toUpperCase()

      if (editingMember) {
        const updated = members.map((m) =>
          m.id === editingMember.id ? { ...m, ...values, initials } : m,
        )
        saveMembers(updated)
      } else {
        const newMember: TeamMember = {
          id: Date.now(),
          name: values.name.trim(),
          initials,
          role: values.role.trim(),
          department: values.department,
          employmentStatus: values.employmentStatus,
          status: values.status,
        }
        saveMembers([...members, newMember])
      }
      setIsModalOpen(false)
    })
  }

  const activeCount = members.filter((m) => m.status === 'Hoạt động').length

  const columns = [
    {
      title: 'Họ và tên',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: TeamMember) => (
        <Space>
          <span className="schedule-avatar" style={{ width: 28, height: 28, fontSize: 10 }}>{record.initials}</span>
          <div>
            <Text strong>{text}</Text>
            <div><Text type="secondary" style={{ fontSize: 12 }}>{record.role}</Text></div>
          </div>
        </Space>
      ),
    },
    {
      title: 'Phòng ban',
      dataIndex: 'department',
      key: 'department',
      render: (dept: string) => <Tag color={dept === 'Phòng Kỹ thuật' ? 'blue' : dept === 'Phòng Nhân sự' ? 'purple' : 'gold'}>{dept}</Tag>,
    },
    {
      title: 'Hợp đồng',
      dataIndex: 'employmentStatus',
      key: 'employmentStatus',
      render: (status: string) => (
        <Tag color={status === 'PERMANENT' ? 'green' : 'orange'}>
          {status === 'PERMANENT' ? 'Chính thức' : 'Thử việc'}
        </Tag>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Badge status={status === 'Hoạt động' ? 'success' : 'default'} text={status} />
      ),
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_: unknown, record: TeamMember) => (
        <Button size="small" type="link" onClick={() => handleOpenEdit(record)}>
          Cập nhật
        </Button>
      ),
    },
  ]

  return (
    <section className="records-page">
      <div className="page-heading" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <span className="page-kicker">QUẢN LÝ NHÂN SỰ & TỔ CHỨC</span>
          <Title level={3} style={{ marginBottom: 4 }}>Danh bạ nhân sự</Title>
          <Text type="secondary">Cập nhật hồ sơ thành viên, phòng ban và đồng bộ tự động với Lịch nhóm.</Text>
        </div>
        <Button type="primary" icon={<TeamOutlined />} onClick={handleOpenAdd}>
          Thêm nhân sự mới
        </Button>
      </div>

      <div className="records-summary">
        <span className="records-summary-icon"><TeamOutlined /></span>
        <div>
          <strong>{activeCount} thành viên đang hoạt động</strong>
          <small>Đồng bộ dữ liệu trực tiếp với toàn bộ Lịch nhóm và Bảng lương HR</small>
        </div>
        <Badge status="success" text="Đồng bộ tự động" />
      </div>

      <Card className="directory-card" variant="borderless">
        <Table
          dataSource={members}
          columns={columns}
          rowKey="id"
          pagination={{ pageSize: 8 }}
          size="middle"
        />
      </Card>

      <Modal
        title={editingMember ? 'Cập nhật nhân sự' : 'Thêm nhân sự mới'}
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={handleModalSubmit}
        okText="Lưu thông tin"
        cancelText="Hủy"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="Họ và tên"
            rules={[{ required: true, message: 'Vui lòng nhập họ và tên' }]}
          >
            <Input placeholder="Ví dụ: Nguyễn Văn A" />
          </Form.Item>
          <Form.Item
            name="role"
            label="Chức danh công việc"
            rules={[{ required: true, message: 'Vui lòng nhập chức danh' }]}
          >
            <Input placeholder="Ví dụ: Kỹ sư phần mềm" />
          </Form.Item>
          <Form.Item
            name="department"
            label="Phòng ban"
            rules={[{ required: true, message: 'Vui lòng chọn phòng ban' }]}
          >
            <Select options={[
              { value: 'Phòng Kỹ thuật', label: 'Phòng Kỹ thuật' },
              { value: 'Phòng Nhân sự', label: 'Phòng Nhân sự' },
              { value: 'Phòng Vận hành', label: 'Phòng Vận hành' },
            ]} />
          </Form.Item>
          <Form.Item
            name="employmentStatus"
            label="Hình thức hợp đồng"
            rules={[{ required: true }]}
          >
            <Select options={[
              { value: 'PERMANENT', label: 'Chính thức (Permanent)' },
              { value: 'PROBATION', label: 'Thử việc (Probation)' },
            ]} />
          </Form.Item>
          <Form.Item
            name="status"
            label="Trạng thái làm việc"
            rules={[{ required: true }]}
          >
            <Select options={[
              { value: 'Hoạt động', label: 'Hoạt động' },
              { value: 'Nghỉ việc', label: 'Nghỉ việc' },
            ]} />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  )
}

function PayrollPage() {
  const [downloading, setDownloading] = useState(false)

  const handleExportCsv = async () => {
    setDownloading(true)
    try {
      const res = await apiFetch('/reports/payroll?month=2026-09&format=csv')
      if (!res.ok) {
        throw new Error('Không thể tải dữ liệu kỳ lương.')
      }
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'bang_luong_thang_09_2026.csv'
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
      message.success('Đã tải xuống bảng lương tháng 09/2026!')
    } catch (err: any) {
      message.error(err.message || 'Lỗi khi tải bảng lương.')
    } finally {
      setDownloading(false)
    }
  }

  return (
    <section className="payroll-page">
      <div className="page-heading">
        <span className="page-kicker">BÁO CÁO & ĐỐI SOÁT</span>
        <Title level={3}>Bảng lương</Title>
        <Text type="secondary">Tổng hợp ngày nghỉ phục vụ kỳ đối soát nhân sự.</Text>
      </div>
      <Row gutter={[16, 16]} className="payroll-summary">
        <Col xs={24} sm={8}><Card className="payroll-stat"><span>Tháng báo cáo</span><strong>09 / 2026</strong><small>Kỳ gần nhất đã chốt</small></Card></Col>
        <Col xs={24} sm={8}><Card className="payroll-stat"><span>Ngày phép có lương</span><strong>128 <small>ngày</small></strong><small>Toàn đơn vị · kỳ báo cáo</small></Card></Col>
        <Col xs={24} sm={8}><Card className="payroll-stat"><span>Nghỉ không lương</span><strong>06 <small>ngày</small></strong><small>Cần đối soát cùng bảng công</small></Card></Col>
      </Row>
      <Card className="payroll-export-panel">
        <div className="export-icon"><CalendarOutlined /></div>
        <div><Title level={5}>Xuất dữ liệu kỳ lương</Title><Text type="secondary">Tải bảng tổng hợp nghỉ phép ở định dạng CSV để tiếp tục đối soát.</Text></div>
        <Button type="primary" loading={downloading} onClick={handleExportCsv}>Tải tệp CSV</Button>
      </Card>
    </section>
  )
}

function ForbiddenPage() {
  const { user } = useAuth()
  const defaultPage = user ? getDefaultPageForRole(user.role) : '/login'

  return (
    <main className="login-page">
      <Card className="state-card" title="Từ chối truy cập">
        <Text>Bạn không có quyền xem trang này.</Text>
        <div style={{ marginTop: 16 }}>
          <Link to={defaultPage}>
            <Button type="primary">Về trang chính</Button>
          </Link>
        </div>
      </Card>
    </main>
  )
}

function AppShell() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const menuItems: { key: string; icon: ReactNode; label: string; roles?: UserRole[] }[] = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Tổng quan' },
    { key: '/calendar', icon: <CalendarOutlined />, label: 'Lịch làm việc' },
    { key: '/approvals', icon: <CalendarOutlined />, label: 'Phê duyệt', roles: ['MANAGER', 'HR'] },
    { key: '/people', icon: <TeamOutlined />, label: 'Nhân sự', roles: ['HR'] },
    { key: '/payroll', icon: <UserOutlined />, label: 'Bảng lương', roles: ['HR'] },
  ]
  const visibleMenuItems = menuItems.filter(
    (item) => !item.roles || (user && item.roles.includes(user.role)),
  )

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true, state: null })
  }

  return (
    <Layout className="app-shell">
      <Header className="topbar">
        <div className="header-inner">
          <Link to="/dashboard" className="brand-lockup" aria-label="NghỉPhép+, về tổng quan">
            <img src="/brand-mark.svg" alt="" />
            <span className="brand-copy">
              <strong>NghỉPhép<span>+</span></strong>
              <small>Quản lý nghỉ phép</small>
            </span>
          </Link>

          <Menu
            mode="horizontal"
            selectedKeys={[location.pathname]}
            onClick={handleMenuClick}
            items={visibleMenuItems}
            className="sidebar-menu"
          />

          <div className="profile-box">
            <div className="profile-copy">
              <Text strong>{user?.name}</Text>
              <Text type="secondary">{user?.role === 'HR' ? 'Nhân sự' : user?.role === 'MANAGER' ? 'Quản lý' : 'Nhân viên'}</Text>
            </div>
            <Button type="text" icon={<LogoutOutlined />} onClick={handleLogout} aria-label="Đăng xuất" />
          </div>
        </div>
        <nav className="mobile-nav" aria-label="Điều hướng chính">
          {visibleMenuItems.map((item) => (
            <Button
              key={item.key}
              type="text"
              className={location.pathname === item.key ? 'active' : undefined}
              icon={item.icon}
              onClick={() => navigate(item.key)}
            >
              {item.label}
            </Button>
          ))}
        </nav>
      </Header>
      <Content className="content-wrap">
        <Outlet />
      </Content>
    </Layout>
  )
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute allowedRoles={['EMPLOYEE', 'MANAGER', 'HR']} />}>
            <Route element={<AppShell />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/calendar" element={<TeamCalendarPage />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'HR']} />}>
            <Route element={<AppShell />}>
              <Route path="/approvals" element={<ApprovalPage />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['HR']} />}>
            <Route element={<AppShell />}>
              <Route path="/people" element={<PeoplePage />} />
              <Route path="/payroll" element={<PayrollPage />} />
            </Route>
          </Route>

          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

const queryClient = new QueryClient()

function RootApp() {
  return (
    <ConfigProvider locale={viVN}>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </ConfigProvider>
  )
}

export default RootApp
