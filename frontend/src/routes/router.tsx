import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { HomePage } from './HomePage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
    ],
  },
])
