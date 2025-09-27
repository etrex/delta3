// ***********************************************************
// This example support/e2e.ts is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands'
import 'cypress-file-upload'

// Make Chai's expect available globally
declare global {
  const expect: Chai.ExpectStatic;
}

// Override cy.select for Element Plus selects
Cypress.Commands.overwrite('select', (originalFn, element, value) => {
  const $el = Cypress.$(element)

  // Check if this is an Element Plus select
  if ($el.hasClass('el-select') || $el.attr('data-cy') === 'role-selector') {
    // Click to open dropdown
    cy.wrap(element).click()

    // Find and click the option with matching value or label
    cy.get('.el-select-dropdown__item').contains(value).click({ force: true })

    return cy.wrap(element)
  }

  // Otherwise use the original select command
  return originalFn(element, value)
})

// Add custom chai assertion for Element Plus select values
chai.Assertion.addMethod('value', function(expected) {
  const $el = this._obj

  // Check if it's an Element Plus select
  if ($el.hasClass('el-select') || $el.attr('data-cy') === 'role-selector') {
    const actual = $el.attr('data-value')
    this.assert(
      actual === expected,
      'expected #{this} to have value #{exp} but got #{act}',
      'expected #{this} not to have value #{exp}',
      expected,
      actual
    )
  } else {
    // Use default value assertion
    const actual = $el.val()
    this.assert(
      actual === expected,
      'expected #{this} to have value #{exp} but got #{act}',
      'expected #{this} not to have value #{exp}',
      expected,
      actual
    )
  }
})

// Alternatively you can use CommonJS syntax:
// require('./commands')