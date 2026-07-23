import { StrictMode, useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import PortfolioStudio from "./PortfolioStudio";
import ProductCase from "./ProductCase";
import Showcase from "./Showcase";
import "./styles.css";

function Root() {
  const resolveView = () => window.location.hash === "#lab" ? "lab" : window.location.hash === "#studio" ? "studio" : window.location.hash === "#product" ? "product" : "showcase";
  const [view, setView] = useState(resolveView);

  useEffect(() => {
    const syncView = () => setView(resolveView());
    window.addEventListener("hashchange", syncView);
    return () => window.removeEventListener("hashchange", syncView);
  }, []);

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [view]);

  const openLab = () => { window.location.hash = "lab"; };
  const openStudio = () => { window.location.hash = "studio"; };
  const openProduct = () => { window.location.hash = "product"; };
  const openShowcase = () => { window.location.hash = ""; };

  if (view === "lab") {
    return <>
      <button type="button" className="lab-home-button" onClick={openShowcase} aria-label="返回学习档案首页">学习档案</button>
      <App />
    </>;
  }

  if (view === "studio") return <PortfolioStudio onOpenLab={openLab} />;
  if (view === "product") return <ProductCase onBack={openShowcase} onOpenLab={openLab} />;

  return <Showcase onOpenLab={openLab} onOpenStudio={openStudio} onOpenProduct={openProduct} />;
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Root />
  </StrictMode>,
);
