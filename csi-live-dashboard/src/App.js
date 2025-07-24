import React, { useEffect, useRef, useState } from 'react';
import styled, { createGlobalStyle } from 'styled-components';
import * as ort from 'onnxruntime-web';

const GlobalStyle = createGlobalStyle`
  body { background: #181c24; color: #fff; font-family: 'Segoe UI', sans-serif; margin: 0; }
`;

const Container = styled.div`
  max-width: 700px; margin: 40px auto; background: #23283b;
  border-radius: 16px; box-shadow: 0 8px 32px #000a; padding: 32px;
`;

const FullScreenSpectrogram = styled.div`
  position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
  background: #111; z-index: 1000; display: flex; flex-direction: column; align-items: center; justify-content: center;
`;

const MenuBtn = styled.button`
  background: linear-gradient(90deg, #00bcd4 0%, #2196f3 100%);
  color: #fff; border: none; border-radius: 8px;
  padding: 16px 32px; margin: 16px 0; font-size: 1.2em; cursor: pointer;
  font-weight: bold; letter-spacing: 1px;
  box-shadow: 0 2px 8px #0004;
  transition: background 0.2s, transform 0.2s;
  &:hover { background: #0097a7; transform: scale(1.04);}
`;

const SpectrogramBox = styled.div`
  margin-top: 24px; background: #111; border-radius: 8px; padding: 16px;
  box-shadow: 0 2px 8px #0006;
`;

const ExpressionBtn = styled.button`
  background: linear-gradient(90deg, #ff9800 0%, #ff5722 100%);
  color: #fff; border: none; border-radius: 8px;
  padding: 14px 0; margin: 8px 8px 8px 0; font-size: 1.1em; cursor: pointer;
  font-weight: bold; width: 48%;
  box-shadow: 0 2px 8px #0003;
  transition: background 0.2s, transform 0.2s;
  &:hover { background: #e65100; transform: scale(1.04);}
`;

const emotionLabels = {
  1: 'Fear',
  2: 'Happy',
  3: 'Sad',
  4: 'Surprised',
  5: 'Neutral',
};

const expressions = [
  { label: 'Fear', value: 'Fear' },
  { label: 'Happy', value: 'Happy' },
  { label: 'Sad', value: 'Sad' },
  { label: 'Surprised', value: 'Surprised' },
  { label: 'Neutral', value: 'Neutral' },
];

// --- CSI amplitude extraction (matches SpectrogramUtils.parseAmplitudeFromCsi) ---
function parseAmplitudeFromCsi(csiString) {
  try {
    const match = csiString.match(/\[([^\]]+)\]/);
    if (!match) return null;
    const parts = match[1].trim().split(/\s+/);
    const n = Math.floor(parts.length / 2);
    const real = [];
    const imag = [];
    for (let i = 0; i < n; i++) {
      real[i] = parseFloat(parts[2 * i]);
      imag[i] = parseFloat(parts[2 * i + 1]);
    }
    const amplitude = [];
    for (let i = 0; i < n; i++) {
      amplitude[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
    }
    return amplitude;
  } catch {
    return null;
  }
}

const SpectrogramCanvas = ({ csiHistory }) => {
  const canvasRef = useRef();
  useEffect(() => {
    if (!canvasRef.current || csiHistory.length === 0) return;
    const ctx = canvasRef.current.getContext('2d');
    // Portrait: width = subcarriers, height = packets
    const width = csiHistory[0]?.length || 64;
    const height = Math.max(120, csiHistory.length); // 120 rows for portrait
    ctx.clearRect(0, 0, width, height);
    // Find min/max for normalization
    let min = Infinity, max = -Infinity;
    for (const row of csiHistory) for (const v of row) {
      if (v < min) min = v;
      if (v > max) max = v;
    }
    for (let y = 0; y < height; y++) {
      const row = csiHistory[y] || [];
      for (let x = 0; x < width; x++) {
        let amp = row[x] || 0;
        let norm = (amp - min) / (max - min + 1e-6);
        // Turbo colormap (blue to yellow)
        const color = `hsl(${240 - norm * 240},100%,50%)`;
        ctx.fillStyle = color;
        ctx.fillRect(x, y, 1, 1);
      }
    }
  }, [csiHistory]);
  // Portrait: width = 64px, height = 120px, scale up for display
  return (
    <div style={{ display: 'flex', justifyContent: 'center' }}>
      <canvas
        ref={canvasRef}
        width={csiHistory[0]?.length || 64}
        height={Math.max(120, csiHistory.length)}
        style={{ width: '40vw', height: '80vw', maxWidth: 320, maxHeight: 640, borderRadius: 8, background: '#000', boxShadow: '0 2px 8px #0006' }}
      />
    </div>
  );
}

function App() {
  const [view, setView] = useState('menu');
  const [csi, setCsi] = useState('');
  const [csiArr, setCsiArr] = useState([]); // latest CSI as array
  const [csiHistory, setCsiHistory] = useState([]); // for spectrogram
  const [csiRawHistory, setCsiRawHistory] = useState([]); // buffer of raw CSI strings
  const [amplitudeHistory, setAmplitudeHistory] = useState([]); // buffer of amplitude arrays
  const [testStep, setTestStep] = useState('select'); // select | running | result
  const [selectedExpr, setSelectedExpr] = useState(null);
  const [timer, setTimer] = useState(5);
  const [detectedEmotion, setDetectedEmotion] = useState(null);
  const [csiStatus, setCsiStatus] = useState('waiting'); // waiting | ok | missing
  const [onnxSession, setOnnxSession] = useState(null);
  const [modelLoading, setModelLoading] = useState(true);
  const timerRef = useRef();
  const csiTimeoutRef = useRef();

  // Load ONNX model on mount
  useEffect(() => {
    async function loadModel() {
      try {
        const session = await ort.InferenceSession.create(process.env.PUBLIC_URL + '/models/emotion_model.onnx');
        setOnnxSession(session);
      } catch (e) {
        // eslint-disable-next-line
        console.error('Failed to load ONNX model:', e);
      } finally {
        setModelLoading(false);
      }
    }
    loadModel();
  }, []);

  // Poll CSI data from the mobile app server
  useEffect(() => {
    let lastCsi = '';
    const fetchCsi = () => {
      fetch('/api/csi')
        .then(res => res.text())
        .then(data => {
          setCsi(data);
          if (data && data !== lastCsi) {
            setCsiStatus('ok');
            clearTimeout(csiTimeoutRef.current);
            csiTimeoutRef.current = setTimeout(() => setCsiStatus('missing'), 2000);
            lastCsi = data;
            // Buffer raw CSI string
            setCsiRawHistory(h => {
              const next = [...h, data];
              if (next.length > 120) next.shift();
              return next;
            });
            // Parse amplitude and buffer
            const amp = parseAmplitudeFromCsi(data);
            setAmplitudeHistory(h => {
              const next = amp ? [...h, amp] : h;
              if (next.length > 120) next.shift();
              return next;
            });
          }
        })
        .catch(() => setCsiStatus('missing'));
    };
    const interval = setInterval(fetchCsi, 100);
    return () => { clearInterval(interval); clearTimeout(csiTimeoutRef.current); };
  }, []);

  // Real-time test timer logic (uses CSI from mobile app)
  useEffect(() => {
    if (testStep === 'running' && timer > 0) {
      timerRef.current = setTimeout(() => setTimer(t => t - 1), 1000);
    } else if (testStep === 'running' && timer === 0) {
      // Use ONNX model for emotion detection
      async function runModel() {
        if (onnxSession && amplitudeHistory.length > 0) {
          // Use the last amplitude array as input
          const inputArr = amplitudeHistory[amplitudeHistory.length - 1];
          // Model expects Float32Array of shape [1, N]
          const inputTensor = new ort.Tensor('float32', Float32Array.from(inputArr), [1, inputArr.length]);
          try {
            const output = await onnxSession.run({ input: inputTensor });
            // Assume output is a logits or probabilities array
            const outArr = output.output.data;
            // Find the index of the max value
            const maxIdx = outArr.indexOf(Math.max(...outArr));
            setDetectedEmotion(emotionLabels[maxIdx + 1] || 'Unknown');
          } catch (e) {
            setDetectedEmotion('Model error');
          }
        } else {
          setDetectedEmotion('No CSI data');
        }
        setTestStep('result');
      }
      runModel();
    }
    return () => clearTimeout(timerRef.current);
  }, [testStep, timer, amplitudeHistory, onnxSession]);

  // Fullscreen spectrogram view
  const [fullscreen, setFullscreen] = useState(false);

  return (
    <>
      <GlobalStyle />
      {modelLoading && (
        <div style={{position:'fixed',top:0,left:0,width:'100vw',height:'100vh',background:'#111a',zIndex:2000,display:'flex',alignItems:'center',justifyContent:'center',fontSize:'2em',color:'#fff'}}>Loading model...</div>
      )}
      {fullscreen ? (
        <FullScreenSpectrogram>
          <MenuBtn style={{position:'absolute',top:20,right:20}} onClick={() => setFullscreen(false)}>Exit Fullscreen</MenuBtn>
          <SpectrogramCanvas csiHistory={amplitudeHistory} />
        </FullScreenSpectrogram>
      ) : (
      <Container>
        {view === 'menu' && (
          <>
            <h1 style={{textAlign:'center'}}>CSI Live Dashboard</h1>
            <MenuBtn onClick={() => setView('test')}>Real-Time Testing</MenuBtn>
            <MenuBtn onClick={() => setView('spectrogram')}>View Live Spectrogram</MenuBtn>
          </>
        )}
        {view === 'test' && (
          <>
            <h2 style={{textAlign:'center'}}>Real-Time Expression Test</h2>
            <div style={{textAlign:'center',marginBottom:8}}>
              <span style={{color: csiStatus === 'ok' ? '#4caf50' : '#ff5252', fontWeight:'bold'}}>
                CSI Status: {csiStatus === 'ok' ? 'Receiving' : 'Not Received'}
              </span>
            </div>
            <MenuBtn onClick={() => {
              setView('menu');
              setTestStep('select');
              setSelectedExpr(null);
              setTimer(5);
              setDetectedEmotion(null);
            }}>Back to Menu</MenuBtn>
            <SpectrogramBox>
              {csiStatus === 'missing' && (
                <div style={{color:'#ff5252',marginBottom:12,fontWeight:'bold'}}>CSI data not received from mobile app!</div>
              )}
              {testStep === 'select' && (
                <>
                  <h3>Select an Expression</h3>
                  <div style={{display:'flex', flexWrap:'wrap', gap:'8px', justifyContent:'space-between'}}>
                    {expressions.map(expr => (
                      <ExpressionBtn key={expr.value} onClick={() => {
                        setSelectedExpr(expr.value);
                        setTestStep('running');
                        setTimer(5);
                        setDetectedEmotion(null);
                      }}>{expr.label}</ExpressionBtn>
                    ))}
                  </div>
                  <p style={{marginTop:16, color:'#aaa'}}>1. Select an expression<br/>2. Perform it for 5 seconds<br/>3. View detection result</p>
                </>
              )}
              {testStep === 'running' && (
                <>
                  <h3>Perform: <span style={{color:'#ff9800'}}>{selectedExpr}</span></h3>
                  <div style={{fontSize:'2.5em', color:'#ff5722', margin:'16px 0'}}>{timer}</div>
                  <p>Keep the expression for 5 seconds...</p>
                  <MenuBtn onClick={() => setFullscreen(true)} style={{margin:'16px 0'}}>Fullscreen Spectrogram</MenuBtn>
                  <SpectrogramCanvas csiHistory={amplitudeHistory} />
                </>
              )}
              {testStep === 'result' && (
                <>
                  <h3>Detection Result</h3>
                  <div style={{fontSize:'2em', color:'#4caf50', margin:'16px 0'}}>{detectedEmotion}</div>
                  <SpectrogramCanvas csiHistory={amplitudeHistory} />
                  <MenuBtn onClick={() => {
                    setTestStep('select');
                    setSelectedExpr(null);
                    setTimer(5);
                    setDetectedEmotion(null);
                  }}>Test Another Expression</MenuBtn>
                </>
              )}
            </SpectrogramBox>
          </>
        )}
        {view === 'spectrogram' && (
          <>
            <h2 style={{textAlign:'center'}}>Live Spectrogram</h2>
            <div style={{textAlign:'center',marginBottom:8}}>
              <span style={{color: csiStatus === 'ok' ? '#4caf50' : '#ff5252', fontWeight:'bold'}}>
                CSI Status: {csiStatus === 'ok' ? 'Receiving' : 'Not Received'}
              </span>
            </div>
            <MenuBtn onClick={() => setView('menu')}>Back to Menu</MenuBtn>
            <MenuBtn onClick={() => setFullscreen(true)} style={{margin:'16px 0'}}>Fullscreen Spectrogram</MenuBtn>
            <SpectrogramBox>
              {csiStatus === 'missing' && (
                <div style={{color:'#ff5252',marginBottom:12,fontWeight:'bold'}}>CSI data not received from mobile app!</div>
              )}
              <SpectrogramCanvas csiHistory={amplitudeHistory} />
            </SpectrogramBox>
          </>
        )}
      </Container>
      )}
    </>
  );
}

export default App;
