import { render, screen } from '@testing-library/react';
import App from '../App';

test('renders app title', () => {
  render(<App />);
  expect(screen.getAllByText(/ATS Resume Tailor/i).length).toBeGreaterThan(0);
});
