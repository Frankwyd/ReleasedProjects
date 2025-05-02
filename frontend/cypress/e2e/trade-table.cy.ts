describe('Trade Table E2E Tests', () => {
  beforeEach(() => {
    cy.visit('http://localhost:3000');
  });

  it('displays trade data and updates in real-time', () => {
    // 验证表格存在
    cy.get('table').should('exist');
    cy.contains('交易明细').should('be.visible');

    // 验证数据加载
    cy.get('table tbody tr').should('have.length.gt', 0);

    // 验证实时更新
    cy.wait(10000); // 等待新数据
    cy.get('table tbody tr').first().should('contain', new Date().getFullYear());
  });

  it('handles manual refresh', () => {
    // 点击刷新按钮
    cy.contains('刷新数据').click();

    // 验证加载状态
    cy.get('.ant-spin').should('exist');

    // 验证数据更新
    cy.get('table tbody tr').should('have.length.gt', 0);
  });

  it('displays error message when connection fails', () => {
    // 模拟断网
    cy.intercept('GET', 'http://localhost:8080/api/trades', {
      forceNetworkError: true
    }).as('getTradesError');

    // 点击刷新按钮
    cy.contains('刷新数据').click();

    // 验证错误提示
    cy.contains('连接服务器失败').should('be.visible');
  });
}); 