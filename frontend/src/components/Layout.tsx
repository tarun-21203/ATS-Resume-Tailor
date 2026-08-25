import { useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import { Box, Button, Container, Typography } from "@mui/material";
import { trackMetric } from "../services/resumeService";

interface LayoutProps {
  children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps): JSX.Element {
  const location = useLocation();
  const isAdminPage = location.pathname === "/admin";

  useEffect(() => {
    if (location.pathname === "/") {
      trackMetric("website_visit").catch(() => undefined);
    }
  }, [location.pathname]);

  return (
    <Box sx={{ minHeight: "100vh", py: 3 }}>
      <Container maxWidth="lg">
        {!isAdminPage && (
          <Box sx={{ mb: 3 }}>
            <Typography variant="h4" fontWeight={700}>ATS Resume Tailor</Typography>
            <Typography variant="body1" color="text.secondary">
              Transform your resume with AI to match any job description
            </Typography>
            <Box sx={{ mt: 1, display: "flex", gap: 1 }}>
              <Button component={Link} to="/admin" variant="outlined" size="small">Admin</Button>
              <Button component={Link} to="/master-resume" variant="outlined" size="small">Master Resume</Button>
            </Box>
          </Box>
        )}
        {children}
      </Container>
    </Box>
  );
}
