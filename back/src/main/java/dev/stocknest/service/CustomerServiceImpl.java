package dev.stocknest.service;

import dev.stocknest.exception.DuplicationException;
import dev.stocknest.exception.IdNotFoundException;
import dev.stocknest.model.dto.CustomerDTO;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.repository.CustomerRepository;
import dev.stocknest.repository.KakaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final KakaoRepository kakaoRepository;

    @Override
    public CustomerDTO login(CustomerDTO dto) {
        Customer customer = customerRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IdNotFoundException("이메일 또는 비밀번호가 틀렸습니다."));

        if (!customer.getPassword().equals(dto.getPassword())) {
            throw new IdNotFoundException("이메일 또는 비밀번호가 틀렸습니다.");
        }

        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .build();
    }

    @Override
    public Customer create(Customer c) {
        if (customerRepository.findByEmail(c.getEmail()).isPresent()) {
            throw new DuplicationException("이미 사용 중인 이메일입니다.");
        }
        return customerRepository.save(c);
    }

    @Override
    public Customer findById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("존재하지 않는 회원입니다."));
    }

    @Override
    public Customer updateCustomerInfo(CustomerDTO dto) {
        Customer customer = findById(dto.getId());
        customer.setName(dto.getName());
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void deleteCustomerInfO(CustomerDTO dto) {
        customerRepository.deleteById(dto.getId());
        kakaoRepository.deleteById(dto.getId());
    }
}
