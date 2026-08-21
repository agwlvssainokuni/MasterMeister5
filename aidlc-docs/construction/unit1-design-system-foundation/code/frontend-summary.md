# Frontend Summary — Unit 1: デザインシステム基盤

## `make-you-chic-ui`実際の統合仕様の確認

submodule取得後、`libs/make-you-chic-ui/docs/integration-guide.md`を読み込み、事前に
application-design-plan.md/nfr-design-plan.mdで想定していた内容とすり合わせた。特に以下は
実装時に重要な発見だった:

- `ThemeProvider`は4軸（`theme`/`brand`/`fontFamily`/`fontSize`）すべてを対称にlocalStorage
  管理する設計であり、「ブランドカラー・フォントだけサーバ管理」という要件との差分は、
  アプリ側で`AppThemeSync`コンポーネントを追加して埋める必要があった
- `resolve.dedupe: ['react', 'react-dom']`が必須（file:参照によるReact二重ロード回避）
- Webフォント（Noto Sans/Serif JP）はライブラリに同梱されないため、`@fontsource/noto-sans-jp`
  `@fontsource/noto-serif-jp`を利用側で追加し、japaneseサブセットをimportする
- `AppShell`は`navItems`必須、ログイン画面等の別レイアウトはreact-routerの
  レイアウトルートパターンで分離する

## 生成したファイル

- `frontend/src/main.tsx`: `ThemeProvider`→`AppThemeSync`→`ToastProvider`→
  `ModalStackProvider`→`BrowserRouter`の順にラップするエントリポイント。
  `make-you-chic-ui/style.css`・Webフォントのimportを含む
- `frontend/src/App.tsx`: ルーティング定義（`AppLayout`をレイアウトルートとし、`/`に
  `HomePage`を配置。Unit 1時点ではこれのみ）
- `frontend/src/layout/AppLayout.tsx`: `AppShell`を使った共通レイアウト
- `frontend/src/routes/HomePage.tsx`: プレースホルダ画面
- `frontend/src/theme/AppThemeSync.tsx`: バックエンドのアプリ全体テーマ設定を
  `ThemeProvider`の`brand`/`fontFamily`へ反映する橋渡しコンポーネント
- `frontend/src/api/theme.ts`: `/api/theme`のfetchクライアント、バックエンドDTOと
  `make-you-chic-ui`の`ThemeBrand`/`ThemeFontFamily`型の相互変換
- `frontend/src/i18n/i18n.ts`・`locales/ja/common.json`・`locales/en/common.json`:
  react-i18nextセットアップ、既定言語は日本語

## 生成したテスト

- `AppThemeSync.test.tsx`: バックエンド取得成功時に`<html>`のdata属性へ反映されること、
  取得失敗時は`ThemeProvider`の既定値のまま保たれることを検証
- `AppLayout.test.tsx`: `AppShell`がレンダリングされ、ナビゲーション文言（i18n経由）と
  子ルートのコンテンツが表示されることを検証

## 既知の制約（Unit 2への申し送り）
- `GET /api/theme`はUnit 1時点では常に401を返す（SecurityConfigに公開エンドポイントが
  ないため）。`AppThemeSync`は取得失敗を静かにフォールバックする設計のため、ログイン前でも
  クラッシュはしないが、実際にサーバ側テーマが反映されるのはUnit 2でログイン機能が
  実装されて以降となる
- 管理者向けのテーマ変更UI（`PUT /api/theme`を呼ぶフォーム）はロール判定の仕組みが
  まだないため、本Unitでは実装していない（`api/theme.ts`の`updateAppTheme`関数は用意済み）
